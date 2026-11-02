package com.substringtechnologies.jpaadmin.security.jdbc;

import com.substringtechnologies.jpaadmin.security.AdminIdentityStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DuplicateKeyException;
import javax.sql.DataSource;
import java.util.*;
import java.util.function.Supplier;

/** Dedicated JDBC tables are deliberately not JPA entities discoverable by generic admin CRUD. */
public class JdbcAdminIdentityStore implements AdminIdentityStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    public JdbcAdminIdentityStore(DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transactions.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try { jdbc.update("INSERT INTO jpa_admin_identity_lock(id) VALUES (1)"); }
        catch (DuplicateKeyException alreadyInitialized) { /* stable cross-instance serialization row */ }
    }
    private <T> T locked(Supplier<T> work) {
        return transactions.execute(status -> {
            jdbc.queryForObject("SELECT id FROM jpa_admin_identity_lock WHERE id=1 FOR UPDATE", Integer.class);
            return work.get();
        });
    }
    public Optional<Account> findByUsername(String username) { return find("username", username); }
    public Optional<Account> findById(String id) { return find("id", id); }
    private Optional<Account> find(String column, String value) {
        return jdbc.query("SELECT * FROM jpa_admin_users WHERE " + column + "=?", (rs, row) -> {
            String id = rs.getString("id");
            return new Account(id, rs.getString("username"), rs.getString("password_hash"), rs.getInt("active") == 1,
                    rs.getInt("admin_access") == 1, rs.getInt("superuser") == 1, rs.getLong("version"),
                    new LinkedHashSet<>(jdbc.queryForList("SELECT group_id FROM jpa_admin_user_groups WHERE user_id=?", String.class, id)),
                    new LinkedHashSet<>(jdbc.queryForList("SELECT DISTINCT p.permission_key FROM jpa_admin_group_permissions p JOIN jpa_admin_user_groups g ON g.group_id=p.group_id WHERE g.user_id=?", String.class, id)));
        }, value).stream().findFirst();
    }
    public List<Account> users() {
        return jdbc.queryForList("SELECT id FROM jpa_admin_users ORDER BY username", String.class).stream().map(id -> findById(id).orElseThrow()).toList();
    }
    public List<Group> groups() {
        return jdbc.query("SELECT * FROM jpa_admin_groups ORDER BY name", (rs, row) -> {
            String id = rs.getString("id");
            return new Group(id, rs.getString("name"), new LinkedHashSet<>(jdbc.queryForList("SELECT permission_key FROM jpa_admin_group_permissions WHERE group_id=?", String.class, id)));
        });
    }
    public void bootstrap(String username, String passwordHash) {
        locked(() -> {
            if (jdbc.queryForObject("SELECT count(*) FROM jpa_admin_users", Long.class) == 0) {
                if (username == null || username.isBlank() || passwordHash == null || passwordHash.isBlank())
                    throw new IllegalStateException("Configure bootstrap admin credentials for the empty identity database");
                insertUser(UUID.randomUUID().toString(), username, passwordHash, true, true, true);
            }
            return null;
        });
    }
    private void insertUser(String id, String username, String hash, boolean active, boolean admin, boolean root) {
        jdbc.update("INSERT INTO jpa_admin_users(id,username,password_hash,active,admin_access,superuser,version) VALUES (?,?,?,?,?,?,0)",
                id, username, hash, active ? 1 : 0, admin ? 1 : 0, root ? 1 : 0);
    }
    public Account saveUser(String id, String username, String hash, boolean active, boolean admin, boolean root, Set<String> groups) {
        return locked(() -> {
            String key = id == null ? UUID.randomUUID().toString() : id;
            if (id == null) {
                if (hash == null) throw new IllegalArgumentException("Password is required for a new user");
                insertUser(key, username, hash, active, admin, root);
            } else {
                var old = findById(id).orElseThrow(() -> new NoSuchElementException("User not found"));
                jdbc.update("UPDATE jpa_admin_users SET username=?,password_hash=?,active=?,admin_access=?,superuser=?,version=? WHERE id=?",
                        username, hash == null ? old.passwordHash() : hash, active ? 1 : 0, admin ? 1 : 0, root ? 1 : 0,
                        old.version() + (hash != null || !username.equals(old.username()) || active != old.active() || admin != old.adminAccess() ? 1 : 0), id);
            }
            jdbc.update("DELETE FROM jpa_admin_user_groups WHERE user_id=?", key);
            for (String group : groups) jdbc.update("INSERT INTO jpa_admin_user_groups(user_id,group_id) VALUES (?,?)", key, group);
            requireSuperuser();
            return findById(key).orElseThrow();
        });
    }
    public void deleteUser(String id) {
        locked(() -> {
            jdbc.update("DELETE FROM jpa_admin_user_groups WHERE user_id=?", id);
            if (jdbc.update("DELETE FROM jpa_admin_users WHERE id=?", id) == 0) throw new NoSuchElementException("User not found");
            requireSuperuser(); return null;
        });
    }
    private void requireSuperuser() {
        if (jdbc.queryForObject("SELECT count(*) FROM jpa_admin_users WHERE active=1 AND admin_access=1 AND superuser=1", Long.class) == 0)
            throw new IllegalArgumentException("At least one active superuser with admin access must remain");
    }
    public Group saveGroup(String id, String name, Set<String> permissions) {
        return locked(() -> {
            String key = id == null ? UUID.randomUUID().toString() : id;
            if (id == null) jdbc.update("INSERT INTO jpa_admin_groups(id,name) VALUES (?,?)", key, name);
            else if (jdbc.update("UPDATE jpa_admin_groups SET name=? WHERE id=?", name, id) == 0) throw new NoSuchElementException("Group not found");
            jdbc.update("DELETE FROM jpa_admin_group_permissions WHERE group_id=?", key);
            for (String permission : permissions) jdbc.update("INSERT INTO jpa_admin_group_permissions(group_id,permission_key) VALUES (?,?)", key, permission);
            return new Group(key, name, Set.copyOf(permissions));
        });
    }
    public void deleteGroup(String id) {
        locked(() -> {
            jdbc.update("DELETE FROM jpa_admin_user_groups WHERE group_id=?", id);
            jdbc.update("DELETE FROM jpa_admin_group_permissions WHERE group_id=?", id);
            if (jdbc.update("DELETE FROM jpa_admin_groups WHERE id=?", id) == 0) throw new NoSuchElementException("Group not found");
            return null;
        });
    }
}
