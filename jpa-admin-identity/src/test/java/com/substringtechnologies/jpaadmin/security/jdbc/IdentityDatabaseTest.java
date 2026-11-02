package com.substringtechnologies.jpaadmin.security.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.substringtechnologies.jpaadmin.security.AdminIdentityStore;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Run only against a disposable database: these tests clear the dedicated identity tables. */
class IdentityDatabaseTest {
    private DriverManagerDataSource source;
    private AdminIdentityStore store;
    private JdbcTemplate jdbc;

    @BeforeEach void initialize() {
        source = new DriverManagerDataSource(
                System.getenv().getOrDefault("IDENTITY_TEST_URL", "jdbc:h2:mem:identity-contract;DB_CLOSE_DELAY=-1"),
                System.getenv().getOrDefault("IDENTITY_TEST_USER", "sa"),
                System.getenv().getOrDefault("IDENTITY_TEST_PASSWORD", ""));
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of("spring.jpa.admin.security.initialize-schema", "true")));
        store = new AdminJdbcAutoConfiguration().adminIdentityStore(source, environment);
        jdbc = new JdbcTemplate(source);
        for (var table : new String[]{"jpa_admin_user_groups", "jpa_admin_group_permissions", "jpa_admin_users", "jpa_admin_groups"})
            jdbc.update("DELETE FROM " + table);
        store.bootstrap("admin", "hash");
    }

    @Test void schemaCanBeInitializedAgainWithoutLosingAccounts() {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of("spring.jpa.admin.security.initialize-schema", "true")));
        var second = new AdminJdbcAutoConfiguration().adminIdentityStore(source, environment);
        second.bootstrap("replacement", "different-hash");
        assertEquals(1, second.users().size());
        assertEquals("hash", second.findByUsername("admin").orElseThrow().passwordHash());
    }

    @Test void groupUnionRevocationAndDeletionWork() {
        var reader = store.saveGroup(null, "Readers", Set.of("catalog.Product:view"));
        var editor = store.saveGroup(null, "Editors", Set.of("catalog.Product:view", "catalog.Product:change"));
        var account = store.saveUser(null, "reader", "hash", true, true, false, Set.of(reader.id(), editor.id()));
        assertEquals(Set.of("catalog.Product:view", "catalog.Product:change"), account.permissions());
        store.saveGroup(editor.id(), "Editors", Set.of());
        assertEquals(Set.of("catalog.Product:view"), store.findById(account.id()).orElseThrow().permissions());
        store.deleteGroup(reader.id());
        assertTrue(store.findById(account.id()).orElseThrow().permissions().isEmpty());
        store.deleteUser(account.id());
        assertTrue(store.findById(account.id()).isEmpty());
    }

    @Test void foreignKeysAndUniquenessAreEnforcedAndChangesRollBack() {
        assertThrows(DataIntegrityViolationException.class, () -> store.saveUser(null, "invalid", "hash", true, true, false, Set.of("missing-group")));
        assertTrue(store.findByUsername("invalid").isEmpty());
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("INSERT INTO jpa_admin_group_permissions(group_id,permission_key) VALUES (?,?)", "missing", "Product:view"));
        var group = store.saveGroup(null, "Readers", Set.of());
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("INSERT INTO jpa_admin_user_groups(user_id,group_id) VALUES (?,?)", "missing", group.id()));
        assertThrows(DuplicateKeyException.class, () -> store.saveGroup(null, "Readers", Set.of()));
        assertThrows(DuplicateKeyException.class, () -> store.saveUser(null, "admin", "hash", true, true, false, Set.of()));
    }

    @Test void usernamesAndPermissionKeysAreCaseSensitiveAndSupportUnicode() {
        var group = store.saveGroup(null, "टीम 🔐", Set.of("catalog.Product:view", "catalog.product:view"));
        assertEquals(2, store.groups().stream().filter(g -> g.id().equals(group.id())).findFirst().orElseThrow().permissions().size());
        store.saveUser(null, "Admin", "hash", true, true, false, Set.of(group.id()));
        assertFalse(store.findByUsername("Admin").orElseThrow().superuser());
        assertTrue(store.findByUsername("admin").orElseThrow().superuser());
    }

    @Test void accountVersionChangesAndLastSuperuserIsProtected() {
        var root = store.findByUsername("admin").orElseThrow();
        var changed = store.saveUser(root.id(), "admin", "new-hash", true, true, true, Set.of());
        assertEquals(root.version() + 1, changed.version());
        assertThrows(IllegalArgumentException.class, () -> store.saveUser(root.id(), "admin", null, false, true, true, Set.of()));
        assertTrue(store.findById(root.id()).orElseThrow().active());
        assertThrows(IllegalArgumentException.class, () -> store.deleteUser(root.id()));
        assertTrue(store.findById(root.id()).isPresent());
    }

    @Test void independentStoresSerializeConcurrentSuperuserChanges() throws Exception {
        var root = store.findByUsername("admin").orElseThrow();
        var other = store.saveUser(null, "other-root", "hash", true, true, true, Set.of());
        var secondStore = new JdbcAdminIdentityStore(source);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> removeAfter(start, store, root.id()));
            var second = executor.submit(() -> removeAfter(start, secondStore, other.id()));
            start.countDown();
            assertEquals(1, first.get(15, TimeUnit.SECONDS) + second.get(15, TimeUnit.SECONDS));
            assertEquals(1, store.users().stream().filter(u -> u.active() && u.adminAccess() && u.superuser()).count());
        } finally { executor.shutdownNow(); }
    }

    private int removeAfter(CountDownLatch start, AdminIdentityStore target, String id) throws InterruptedException {
        start.await();
        try { target.deleteUser(id); return 1; }
        catch (IllegalArgumentException expected) { return 0; }
    }
}
