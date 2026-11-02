# Custom identity stores

Use this extension when your application needs a database unsupported by the built-in identity module, a separately managed admin datasource, or another persistence implementation. This guide describes the 0.2.0-SNAPSHOT API.

## Register the persistence adapter

`AdminIdentityStore` lives in `jpa-admin-core`, which the regular starter already brings in. The optional `jpa-admin-identity` dependency is not required when you provide a complete custom store.

```java
import com.substringtechnologies.jpaadmin.security.AdminIdentityStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
public class AdminIdentityConfiguration {
    @Bean
    AdminIdentityStore adminIdentityStore(DataSource dataSource) {
        return new AcmeAdminIdentityStore(dataSource);
    }
}
```

`AcmeAdminIdentityStore` is your implementation, not a library class. Implement every method below; the configuration snippet alone is not an implementation. Keep this configuration in Spring's component scan or import it explicitly.

```yaml
spring:
  jpa:
    admin:
      security:
        mode: database
        initialize-schema: false
        username: ${ADMIN_BOOTSTRAP_USERNAME:admin}
        password: ${ADMIN_BOOTSTRAP_PASSWORD:}
```

Even for a custom store, the mode is `database`; there is no `custom` mode. A fresh empty store requires bootstrap credentials. Your store owns migrations and initialization. `initialize-schema` configures only the built-in JDBC adapter.

The built-in bean is conditional on a missing `AdminIdentityStore`. A normal application bean makes it back off, including its vendor detection and schema initialization. If you package your adapter as a separate auto-configuration, order it before `AdminJdbcAutoConfiguration` so that its bean definition is visible in time. Provide one unambiguous identity-store bean; multiple unqualified candidates can prevent startup.

## Contract to implement

The exact source is [AdminIdentityStore.java](../jpa-admin-core/src/main/java/com/substringtechnologies/jpaadmin/security/AdminIdentityStore.java).

| Method | Required behavior |
| --- | --- |
| `findByUsername(username)` | Return the matching account or `Optional.empty()`; account includes password hash and current effective grants. |
| `findById(id)` | Resolve the current account by stable ID. Used to validate existing sessions and current grants. |
| `users()` | List accounts for management. The controller returns safe DTOs, omitting hashes. |
| `groups()` | Return groups with their exact grant sets. |
| `bootstrap(username, encodedPassword)` | Create the initial active superuser only if no accounts exist. Never overwrite existing accounts on restart. |
| `saveUser(id, ...)` | Null ID means create; existing ID means update. Replace memberships atomically and return the current account. |
| `deleteUser(id)` | Delete the account and memberships atomically, preserving at least one active superuser with admin access. |
| `saveGroup(id, name, permissions)` | Create/update the group and replace its complete permission set atomically. |
| `deleteGroup(id)` | Remove the group, memberships and grants atomically. Users remain; they lose that group's grants. |

An `Account` contains `id`, `username`, `passwordHash`, `active`, `adminAccess`, `superuser`, `version`, `groupIds` and `permissions`. A `Group` contains `id`, `name` and `permissions`.

### Passwords and sessions

- The service passes an **already BCrypt-encoded** password. Store it as supplied; do not encode it again or log it.
- New accounts require a hash. On update, a null hash means retain the existing hash.
- IDs are stable strings. Do not reuse a deleted user's ID for a different person.
- Increment `version` on password reset, username change, active-status change or admin-access change. Existing sessions carry the old version and are rejected.
- Group grants and superuser status are read from the current account. Return fresh data on subsequent requests so revocation is effective. A long-lived permissions cache needs explicit invalidation; the built-in service only caches resolution within one request.
- Keep persistence failures distinct from a missing user. Do not turn a database outage into a valid fallback account.

### Authorization and data integrity

`permissions` is the union of grants from all assigned groups, with duplicates removed. Keys are exact and case-sensitive. The core checks View prerequisites, entity restrictions and each requested operation; your adapter must return correct current grants.

Preserve unique usernames/group names, valid memberships, and referential integrity. A failed membership or grant update must roll back the whole change. Preserve the last active, admin-enabled superuser even if two application instances modify different superusers concurrently. An in-process `synchronized` block alone does not protect multiple instances.

The HTTP controller validates inputs, but adapter integrity must also hold for bootstrap, concurrent writes and direct Java callers. Use the same exception conventions where applicable: `NoSuchElementException` for missing records, `IllegalArgumentException` for rejected business rules, and Spring `DataIntegrityViolationException` for database conflicts. These map to 404, 400 and 409 in the management API. Avoid exposing SQL or secrets in exception messages.

### Keep admin identity separate

Store admin identities separately from customer/host accounts if that is your requirement. A custom adapter need not reuse the host's users table. If implemented with JPA entities, annotate the admin account/group entities with `@AdminResource(hidden = true)` so generic entity APIs cannot expose or mutate them. Hiding a field in a form is not a replacement for isolating identity tables.

For a dedicated datasource, construct the adapter with the intended qualified datasource explicitly and own its transaction manager and migrations. There is no built-in second-datasource configuration namespace.

## What remains supplied by the library

The library still supplies session login, CSRF handling, admin/host authentication isolation, Users and Groups UI, management endpoints, metadata filtering and CRUD/action/upload permission enforcement. You are replacing persistence, not rebuilding the UI or accepting host JWTs automatically.

`AdminAuthorization` is a different extension: it changes entity authorization decisions. It does not replace account persistence or make ordinary users security administrators. Management endpoints still use the built-in identity service's superuser checks. Start with `AdminIdentityStore` when the requirement is another database.

## Adapter acceptance checklist

Run tests against the real target database, not only an in-memory compatibility mode:

- Empty bootstrap and repeated startup; no password overwrite or duplicate initial account.
- Create/edit/delete users and groups; union of grants from multiple groups.
- Null password on update, password-reset version changes and account disablement.
- Membership/grant revocation visible to existing sessions on their next request.
- Username/group uniqueness, Unicode and case behavior.
- Missing references and transaction rollback after partial writes.
- Concurrent attempts to remove or disable the final two superusers: exactly one may succeed.
- An ordinary user cannot call security management APIs or bypass entity permissions.

Use [IdentityDatabaseTest.java](../jpa-admin-identity/src/test/java/com/substringtechnologies/jpaadmin/security/jdbc/IdentityDatabaseTest.java) as a persistence contract reference and the sample's `AdminPermissionsTest` for HTTP/session behavior. Adapt the setup for your schema; never point destructive contract tests at production.
