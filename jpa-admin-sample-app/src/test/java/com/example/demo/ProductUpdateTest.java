package com.example.demo;

import io.github.jpaadmin.service.DynamicCrudService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ProductUpdateTest {

    @Autowired
    private DynamicCrudService crudService;

    @Autowired
    private io.github.jpaadmin.metadata.JpaMetamodelScanner scanner;

    @Test
    void testUpdatingProductWithCreatedAtDoesNotFail() {
        // First get record 1
        Map<String, Object> record = crudService.getRecord("Product", 1L);
        assertNotNull(record);
        Object originalCreatedAt = record.get("createdAt");
        assertNotNull(originalCreatedAt);

        // Update payload with new name AND string createdAt
        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("name", "Updated Product Name");
        updatePayload.put("createdAt", "2026-10-17T15:25:27.000Z");

        Map<String, Object> updated = crudService.updateRecord("Product", 1L, updatePayload);
        assertEquals("Updated Product Name", updated.get("name"));
        // createdAt should remain original because it's read-only
        assertEquals(originalCreatedAt, updated.get("createdAt"));
    }
}
