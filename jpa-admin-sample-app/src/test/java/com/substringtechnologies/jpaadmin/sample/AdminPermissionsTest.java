package com.substringtechnologies.jpaadmin.sample;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.substringtechnologies.jpaadmin.security.AdminIdentityStore;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.jpa.admin.security.mode=database", "spring.jpa.admin.security.initialize-schema=true",
        "spring.jpa.admin.security.username=root", "spring.jpa.admin.security.password=root-password-123",
        "spring.datasource.url=jdbc:h2:mem:permissions;DB_CLOSE_DELAY=-1", "logging.level.root=WARN"})
class AdminPermissionsTest {
    @Autowired WebApplicationContext context;
    @Autowired AdminIdentityStore store;
    MockMvc mvc;
    MockHttpSession root;
    final ObjectMapper json = new ObjectMapper();
    static final String PASSWORD = "test-password-123";
    static final String DOMAIN = "com.substringtechnologies.jpaadmin.sample.domain.";
    @BeforeEach void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        root = login("root", "root-password-123");
    }
    MockHttpSession login(String username, String password) throws Exception {
        var session = new MockHttpSession();
        mvc.perform(write(post("/admin/api/auth/login").param("username", username).param("password", password), session))
                .andExpect(status().isNoContent());
        return session;
    }
    MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder request, MockHttpSession session) throws Exception {
        var result = mvc.perform(get("/admin/api/auth/session").session(session)).andExpect(status().isOk()).andReturn();
        var token = (CsrfToken) result.getRequest().getAttribute(CsrfToken.class.getName());
        return request.session(session).header(token.getHeaderName(), token.getToken());
    }
    JsonNode send(MockHttpServletRequestBuilder request, MockHttpSession session, Object body, int expected) throws Exception {
        var result = mvc.perform(write(request.contentType("application/json").content(json.writeValueAsString(body)), session))
                .andExpect(status().is(expected)).andReturn();
        return result.getResponse().getContentAsString().isEmpty() ? json.nullNode() : json.readTree(result.getResponse().getContentAsString());
    }
    JsonNode group(String... grants) throws Exception {
        return send(post("/admin/api/security/groups"), root, Map.of("name", "group-" + UUID.randomUUID(), "permissions", grants), 200);
    }
    JsonNode user(String... groups) throws Exception {
        return send(post("/admin/api/security/users"), root, Map.of("username", "user-" + UUID.randomUUID(), "password", PASSWORD,
                "active", true, "adminAccess", true, "superuser", false, "groupIds", groups), 200);
    }
    @Test void defaultDenyAndManagementCannotBeEscalated() throws Exception {
        var user = user(); var session = login(user.get("username").asText(), PASSWORD);
        mvc.perform(get("/admin/api/metadata").session(session)).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/admin/api/Product").session(session)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        send(post("/admin/api/security/groups"), session, Map.of("name", "escalate", "permissions", List.of()), 403);
        send(put("/admin/api/security/users/" + user.get("id").asText()), session,
                Map.of("username", user.get("username").asText(), "superuser", true, "active", true, "adminAccess", true), 403);
        mvc.perform(get("/admin/api/security/users").session(root)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("passwordHash"))));
    }
    @Test void viewOnlyIsEnforcedAcrossCrudStatsRelationsAndUploads() throws Exception {
        var group = group(DOMAIN + "Product:view"); var user = user(group.get("id").asText());
        var session = login(user.get("username").asText(), PASSWORD);
        mvc.perform(get("/admin/api/metadata").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].permissions.change").value(false))
                .andExpect(jsonPath("$[0].actions.length()").value(0));
        mvc.perform(get("/admin/api/Product/1").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.category").doesNotExist()).andExpect(jsonPath("$.tags").doesNotExist());
        mvc.perform(get("/admin/api/Category/lookup").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/api/Product?category=1").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/api/Product?sort=category,asc").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/api/stats").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.counts.Category").doesNotExist());
        mvc.perform(get("/admin/api/stats/timeline").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.series.Customer").doesNotExist());
        send(post("/admin/api/Product"), session, Map.of(), 403);
        send(put("/admin/api/Product/1"), session, Map.of("name", "not allowed"), 403);
        send(delete("/admin/api/Product/1"), session, Map.of(), 403);
        send(post("/admin/api/Product/batch-delete"), session, List.of(1), 403);
        send(post("/admin/api/uploads/prepare"), session, Map.of("entity", "Product", "field", "imageUrl", "fileName", "a.png", "contentType", "image/png", "size", 10), 403);
        // A second principal's metadata must not inherit the reader's restrictions.
        mvc.perform(get("/admin/api/metadata").session(root)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(6));
    }
    @Test void groupUnionAndRevocationApplyToExistingSession() throws Exception {
        var read = group(DOMAIN + "Category:view"); var edit = group(DOMAIN + "Category:change");
        var user = user(read.get("id").asText(), edit.get("id").asText()); var session = login(user.get("username").asText(), PASSWORD);
        send(put("/admin/api/Category/1"), session, Map.of("description", "Permission test"), 200);
        send(put("/admin/api/security/groups/" + edit.get("id").asText()), root, Map.of("name", edit.get("name").asText(), "permissions", List.of()), 200);
        send(put("/admin/api/Category/1"), session, Map.of("description", "Denied"), 403);
        mvc.perform(get("/admin/api/Category/1").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.description").value("Permission test"));
    }
    @Test void resetAndDisableRevokeExistingSessions() throws Exception {
        var user = user(); var session = login(user.get("username").asText(), PASSWORD);
        var updated = new HashMap<String,Object>(); updated.put("username", user.get("username").asText()); updated.put("superuser", false); updated.put("active", true); updated.put("adminAccess", true); updated.put("password", "replacement-password-123");
        send(put("/admin/api/security/users/" + user.get("id").asText()), root, updated, 200);
        mvc.perform(get("/admin/api/auth/session").session(session)).andExpect(jsonPath("$.authenticated").value(false));
        session = login(user.get("username").asText(), "replacement-password-123");
        updated.remove("password"); updated.put("active", false);
        send(put("/admin/api/security/users/" + user.get("id").asText()), root, updated, 200);
        mvc.perform(get("/admin/api/auth/session").session(session)).andExpect(jsonPath("$.authenticated").value(false));
    }
    @Test void lastSuperuserAndUnknownPermissionsAreRejected() throws Exception {
        var rootUser = store.findByUsername("root").orElseThrow();
        send(delete("/admin/api/security/users/" + rootUser.id()), root, Map.of(), 400);
        send(put("/admin/api/security/users/" + rootUser.id()), root, Map.of("username", "root", "active", false, "superuser", true, "adminAccess", true), 400);
        assertTrue(store.findByUsername("root").orElseThrow().active());
        send(post("/admin/api/security/groups"), root, Map.of("name", "invalid", "permissions", List.of("unknown:view")), 400);
    }
    @Test void uploadsAreBoundToOwnerAndRecheckedAfterRevocation() throws Exception {
        var group = group(DOMAIN + "Product:view", DOMAIN + "Product:change");
        var first = user(group.get("id").asText()); var second = user(group.get("id").asText());
        var one = login(first.get("username").asText(), PASSWORD); var two = login(second.get("username").asText(), PASSWORD);
        var upload = send(post("/admin/api/uploads/prepare"), one,
                Map.of("entity", "Product", "field", "imageUrl", "fileName", "a.png", "contentType", "image/png", "size", 10), 200);
        String token = upload.get("token").asText();
        send(post("/admin/api/uploads/complete"), two, Map.of("token", token), 403);
        mvc.perform(write(put("/admin/api/uploads/local/" + token).content("fake"), two)).andExpect(status().isForbidden());
        send(put("/admin/api/security/groups/" + group.get("id").asText()), root, Map.of("name", group.get("name").asText(), "permissions", List.of(DOMAIN + "Product:view")), 200);
        send(post("/admin/api/uploads/complete"), one, Map.of("token", token), 403);
    }
    @Test void nestedWritesNeedChildPermissionsAndRollBackParent() throws Exception {
        var group = group(DOMAIN + "Order:view", DOMAIN + "Order:change");
        var user = user(group.get("id").asText()); var session = login(user.get("username").asText(), PASSWORD);
        mvc.perform(get("/admin/api/Order/1").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.items").doesNotExist());
        var before = json.readTree(mvc.perform(get("/admin/api/Order/1").session(root)).andReturn().getResponse().getContentAsString());
        send(put("/admin/api/Order/1"), session, Map.of("orderNumber", "MUST-ROLL-BACK", "items", List.of()), 403);
        mvc.perform(get("/admin/api/Order/1").session(root)).andExpect(jsonPath("$.orderNumber").value(before.get("orderNumber").asText()));
    }
    @Test void actionsNeedExplicitGrant() throws Exception {
        var group = group(DOMAIN + "Product:view", DOMAIN + "Product:change");
        var user = user(group.get("id").asText()); var session = login(user.get("username").asText(), PASSWORD);
        var metadata = json.readTree(mvc.perform(get("/admin/api/metadata").session(root)).andReturn().getResponse().getContentAsString());
        String action = null;
        for (var entity : metadata) if (entity.get("entityName").asText().equals("Product")) action = entity.get("actions").get(0).get("id").asText();
        assertNotNull(action);
        send(post("/admin/api/Product/actions/" + action), session, List.of(1), 403);
        send(put("/admin/api/security/groups/" + group.get("id").asText()), root,
                Map.of("name", group.get("name").asText(), "permissions", List.of(DOMAIN + "Product:view", DOMAIN + "Product:action:" + action)), 200);
        send(post("/admin/api/Product/actions/" + action), session, List.of(1), 200);
    }
    @Test void deletingOwnedChildrenNeedsTheirDeleteGrant() throws Exception {
        var group = group(DOMAIN + "Order:view", DOMAIN + "Order:delete");
        var user = user(group.get("id").asText()); var session = login(user.get("username").asText(), PASSWORD);
        var created = send(post("/admin/api/Order"), root, Map.of("orderNumber", "CASCADE-" + UUID.randomUUID(),
                "status", "PENDING", "totalAmount", "10", "customer", Map.of("id", 1),
                "items", List.of(Map.of("product", Map.of("id", 1), "quantity", 1, "unitPrice", "10"))), 201);
        String id = created.get("id").asText();
        send(delete("/admin/api/Order/" + id), session, Map.of(), 403);
        send(post("/admin/api/Order/batch-delete"), session, List.of(id), 403);
        mvc.perform(get("/admin/api/Order/" + id).session(root)).andExpect(status().isOk());
        send(delete("/admin/api/Order/" + id), root, Map.of(), 204);
    }

    @Test void concurrentChangesCannotRemoveAllSuperusers() throws Exception {
        var original = store.findByUsername("root").orElseThrow();
        var first = store.saveUser(null, "parallel-a-" + UUID.randomUUID(), original.passwordHash(), true, true, true, Set.of());
        var second = store.saveUser(null, "parallel-b-" + UUID.randomUUID(), original.passwordHash(), true, true, true, Set.of());
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            store.saveUser(original.id(), original.username(), null, false, true, true, Set.of());
            var start = new java.util.concurrent.CountDownLatch(1);
            var tasks = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (String id : List.of(first.id(), second.id())) tasks.add(pool.submit(() -> {
                start.await();
                try { store.deleteUser(id); return true; }
                catch (IllegalArgumentException expected) { return false; }
            }));
            start.countDown();
            int deleted = 0;
            for (var task : tasks) if (task.get(10, java.util.concurrent.TimeUnit.SECONDS)) deleted++;
            assertEquals(1, deleted);
        } finally {
            pool.shutdownNow();
            store.saveUser(original.id(), original.username(), null, true, true, true, Set.of());
            if (store.findById(first.id()).isPresent()) store.deleteUser(first.id());
            if (store.findById(second.id()).isPresent()) store.deleteUser(second.id());
        }
    }

}
