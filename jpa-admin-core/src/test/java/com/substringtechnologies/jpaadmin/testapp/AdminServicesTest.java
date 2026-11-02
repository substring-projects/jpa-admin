package com.substringtechnologies.jpaadmin.testapp;

import com.substringtechnologies.jpaadmin.annotations.InlineType;
import com.substringtechnologies.jpaadmin.metadata.EntityMetadata;
import com.substringtechnologies.jpaadmin.metadata.JpaMetamodelScanner;
import com.substringtechnologies.jpaadmin.service.AdminValidationException;
import com.substringtechnologies.jpaadmin.service.DynamicCrudService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AdminServicesTest {
    @Autowired DynamicCrudService crud;
    @Autowired JpaMetamodelScanner scanner;
    @Autowired EntityManager entityManager;
    @Autowired TransactionTemplate transactions;

    private EntityMetadata account() { return scanner.getMetadata("Account"); }

    private Object createAccount(Map<String, Object> values) {
        var payload = new HashMap<String, Object>(Map.of("username", "user-" + UUID.randomUUID()));
        payload.putAll(values);
        return crud.createRecord("Account", payload).get("id");
    }

    private Account stored(Object id) {
        return transactions.execute(status -> entityManager.find(Account.class, ((Number) id).longValue()));
    }

    @Test
    void widgetOptionsAndInlineLayoutReachMetadata() {
        var rating = account().getField("rating");
        assertEquals(0.0, rating.getMin());
        assertEquals(5.0, rating.getMax());
        assertEquals(0.5, rating.getStep());
        assertEquals(10.0, account().getField("seats").getMin());
        assertNull(account().getField("seats").getMax());
        assertEquals("sql", account().getField("query").getLanguage());
        assertEquals("64px", account().getField("avatarUrl").getPreviewSize());
        assertEquals(InlineType.STACKED, account().getInlines().get(0).getType());
        assertEquals("code", account().getField("team").getSearchProperty());
    }

    @Test
    void pageSizeComesFromAnnotationOrGlobalDefault() {
        assertEquals(15, account().getPageSize());
        assertEquals(TestApplication.DEFAULT_PAGE_SIZE, scanner.getMetadata("Team").getPageSize());
    }

    @Test
    void configurerOverridesAnnotationDefaults() {
        var team = scanner.getMetadata("Team");
        assertEquals("Squad", team.getDisplayName());
        assertEquals("Squads", team.getPluralName());
        assertEquals("users", team.getIcon());
        assertEquals(3, team.getOrder());
    }

    @Test
    void sensitiveValuesNeverLeaveTheServer() {
        Object id = createAccount(Map.of("password", "hunter2-secret", "apiKey", "key-123", "recoveryCode", "rc-1"));
        Map<String, Object> record = crud.getRecord("Account", id);
        assertEquals("••••••••", record.get("password"));
        assertEquals("[set]", record.get("recoveryCode"));
        assertFalse(record.containsKey("apiKey"), "mask = false must not reveal whether a value is set");
        assertNull(account().getField("apiKey").getMaskPattern());
        assertFalse(account().getField("password").isSortable());
        assertFalse(account().getField("password").isFilterable());
    }

    @Test
    void maskOrBlankSentBackKeepsTheStoredValue() {
        Object id = createAccount(Map.of("password", "original-pass", "recoveryCode", "rc-original"));
        crud.updateRecord("Account", id, new HashMap<>(Map.of("password", "••••••••", "recoveryCode", "[set]")));
        var payload = new HashMap<String, Object>();
        payload.put("password", "");
        payload.put("recoveryCode", null);
        crud.updateRecord("Account", id, payload);
        assertEquals("original-pass", stored(id).getPassword());
        assertEquals("rc-original", stored(id).getRecoveryCode());

        crud.updateRecord("Account", id, new HashMap<>(Map.of("password", "changed-pass")));
        assertEquals("changed-pass", stored(id).getPassword());
    }

    @Test
    @SuppressWarnings("unchecked")
    void inlineRowsDoNotSaveTheMaskOverTheirSecrets() {
        Object id = createAccount(Map.of("credentials", List.of(Map.of("label", "ci", "secret", "ci-token"))));
        var rows = (List<Map<String, Object>>) crud.getRecord("Account", id).get("credentials");
        assertEquals("••••••••", rows.get(0).get("secret"));
        crud.updateRecord("Account", id, new HashMap<>(Map.of("credentials", List.of(new HashMap<>(rows.get(0))))));
        String secret = transactions.execute(status -> entityManager.find(Account.class, ((Number) id).longValue()).getCredentials().get(0).getSecret());
        assertEquals("ci-token", secret);
    }

    @Test
    void widgetBoundsAreEnforcedOnSave() {
        var error = assertThrows(AdminValidationException.class, () -> createAccount(Map.of("rating", "7", "seats", 3)));
        assertEquals("Must be at most 5", error.getFieldErrors().get("rating"));
        assertEquals("Must be at least 10", error.getFieldErrors().get("seats"));
        assertNotNull(createAccount(Map.of("rating", "4.5", "seats", 10)));
    }

    @Test
    void computedValuesUseTheirFormat() {
        Object id = createAccount(Map.of("rating", "4.5"));
        assertEquals("90.0%", crud.getRecord("Account", id).get("score"));
    }

    @Test
    void sortingBySensitiveFieldFallsBackToTheDefaultOrder() {
        createAccount(Map.of("password", "aaa-first"));
        createAccount(Map.of("password", "zzz-last"));
        var bySecret = crud.listRecords("Account", PageRequest.of(0, 50, Sort.by("password")), null, Map.of());
        var byDefault = crud.listRecords("Account", PageRequest.of(0, 50), null, Map.of());
        assertEquals(byDefault.getContent().stream().map(r -> r.get("id")).toList(),
                bySecret.getContent().stream().map(r -> r.get("id")).toList());
    }

    @Test
    void lookupSearchesAndLabelsWithTheConfiguredProperties() {
        var team = crud.createRecord("Team", new HashMap<>(Map.of("name", "Platform", "code", "PLT-" + UUID.randomUUID())));
        String code = (String) crud.getRecord("Team", team.get("id")).get("code");
        var options = crud.lookup("Team", code.substring(0, 8), 10, "code", "name");
        assertTrue(options.stream().anyMatch(o -> o.get("label").equals("Platform")), options.toString());
        assertTrue(crud.lookup("Team", "Platform", 10, "code", "name").isEmpty(), "name must not be searched when searchProperty is code");
        assertThrows(IllegalArgumentException.class, () -> crud.lookup("Account", "x", 10, "password", null));
        assertThrows(IllegalArgumentException.class, () -> crud.lookup("Account", "x", 10, null, "apiKey"));
    }
}
