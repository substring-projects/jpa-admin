package com.substringtechnologies.jpaadmin.sample;

import com.substringtechnologies.jpaadmin.metadata.EntityMetadata;
import com.substringtechnologies.jpaadmin.metadata.FieldMetadata;
import com.substringtechnologies.jpaadmin.metadata.FieldType;
import com.substringtechnologies.jpaadmin.metadata.JpaMetamodelScanner;
import com.substringtechnologies.jpaadmin.service.AdminValidationException;
import com.substringtechnologies.jpaadmin.service.DynamicCrudService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CrudFeaturesTest {

    @Autowired
    private DynamicCrudService crud;

    @Autowired
    private JpaMetamodelScanner scanner;

    private long count(String entity, Map<String, String> filters) {
        return crud.listRecords(entity, PageRequest.of(0, 50), null, filters).getTotalElements();
    }

    @Test
    void invalidValuesAreReportedPerField() {
        var error = assertThrows(AdminValidationException.class, () -> crud.createRecord("Product",
                new HashMap<>(Map.of("name", "", "price", "-5", "stockQuantity", "abc", "status", "DRAFT"))));
        assertTrue(error.getFieldErrors().containsKey("name"), error.getFieldErrors().toString());
        assertTrue(error.getFieldErrors().containsKey("price"), error.getFieldErrors().toString());
        assertEquals("Enter a valid number", error.getFieldErrors().get("stockQuantity"));
    }

    @Test
    void rejectedUpdateLeavesRecordUnchanged() {
        String before = (String) crud.getRecord("Category", 2L).get("name");
        assertThrows(AdminValidationException.class, () -> crud.updateRecord("Category", 2L, new HashMap<>(Map.of("name", " "))));
        assertEquals(before, crud.getRecord("Category", 2L).get("name"));
    }

    @Test
    void filtersSupportRelationsRangesAndTextContains() {
        assertEquals(3, count("Product", Map.of("category", "1")));
        assertEquals(3, count("Product", Map.of("price__gte", "1000")));
        assertEquals(2, count("Product", Map.of("price__gte", "400", "price__lte", "1200")));
        assertEquals(1, count("Customer", Map.of("email", "CYBERDYNE")));
        String today = LocalDate.now().toString();
        assertEquals(count("Product", Map.of()), count("Product", Map.of("createdAt__gte", today, "createdAt__lte", today)));
        assertEquals(0, count("Product", Map.of("createdAt__lte", LocalDate.now().minusDays(1).toString())));
        assertThrows(IllegalArgumentException.class, () -> count("Product", Map.of("price__gte", "lots")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void inlineRowsAreCreatedUpdatedAndRemovedWithTheParent() {
        Map<String, Object> payload = new HashMap<>(Map.of(
                "orderNumber", "ORD-TEST-" + UUID.randomUUID(), "customer", Map.of("id", 1), "status", "PENDING", "totalAmount", "10",
                "items", List.of(Map.of("product", Map.of("id", 1), "quantity", 2, "unitPrice", "5"),
                        Map.of("product", Map.of("id", 2), "quantity", 1, "unitPrice", "7"))));
        Map<String, Object> created = crud.createRecord("Order", payload);
        Object orderId = created.get("id");
        List<Map<String, Object>> items = (List<Map<String, Object>>) crud.getRecord("Order", orderId).get("items");
        assertEquals(2, items.size());

        Map<String, Object> kept = new HashMap<>(items.get(0));
        kept.put("quantity", 9);
        crud.updateRecord("Order", orderId, new HashMap<>(Map.of("items", List.of(kept))));
        items = (List<Map<String, Object>>) crud.getRecord("Order", orderId).get("items");
        assertEquals(1, items.size());
        assertEquals(9, ((Number) items.get(0).get("quantity")).intValue());

        var error = assertThrows(AdminValidationException.class, () -> crud.updateRecord("Order", orderId,
                new HashMap<>(Map.of("items", List.of(Map.of("product", Map.of("id", 1), "quantity", 0, "unitPrice", "5"))))));
        assertTrue(error.getFieldErrors().containsKey("items[0].quantity"), error.getFieldErrors().toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void manyToManyIsEditableOnTheOwningSide() {
        EntityMetadata product = scanner.getMetadata("Product");
        FieldMetadata tags = product.getField("tags");
        assertEquals(FieldType.RELATION_MANY_TO_MANY, tags.getFieldType());
        assertEquals("Tag", tags.getLookupTargetEntity());
        assertTrue(tags.isInForm());

        crud.updateRecord("Product", 5L, new HashMap<>(Map.of("tags", List.of(Map.of("id", 1), Map.of("id", 4)))));
        List<Map<String, Object>> saved = (List<Map<String, Object>>) crud.getRecord("Product", 5L).get("tags");
        assertEquals(Set.of("Bestseller", "Clearance"), new HashSet<>(saved.stream().map(t -> t.get("label")).toList()));

        var error = assertThrows(AdminValidationException.class,
                () -> crud.updateRecord("Product", 5L, new HashMap<>(Map.of("tags", List.of(Map.of("id", 999))))));
        assertEquals("The selected record no longer exists", error.getFieldErrors().get("tags"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void timelineCountsRecordsCreatedToday() {
        Map<String, Object> timeline = crud.creationTimeline(7);
        List<String> days = (List<String>) timeline.get("days");
        assertEquals(7, days.size());
        assertEquals(LocalDate.now().toString(), days.get(6));
        Map<String, List<Long>> series = (Map<String, List<Long>>) timeline.get("series");
        assertTrue(series.get("Product").get(6) >= 5, series.toString());
    }
}
