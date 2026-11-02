# Users, groups and entity permissions

The default `configured` security mode keeps the existing single admin account. To manage multiple admins with Django-style groups and entity permissions, explicitly enable `database` mode and add the optional JDBC identity module. Host application authentication stays separate in both modes.

## Enable database mode

Add this alongside the starter, using the same library version:

```xml
<dependency>
    <groupId>com.substringtechnologies</groupId>
    <artifactId>jpa-admin-identity</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

```yaml
spring:
  jpa:
    admin:
      security:
        mode: database
        username: ${ADMIN_BOOTSTRAP_USERNAME:admin}
        password: ${ADMIN_BOOTSTRAP_PASSWORD}
        initialize-schema: false
```

Apply the bundled `jpa-admin/migrations/V1__admin_identity.sql` using your application's migration process before starting. It creates dedicated JDBC tables, not JPA entities, so the generic entity APIs cannot expose users or password hashes. The DDL targets H2 and PostgreSQL syntax; automated database tests currently use H2. Verify migrations and locking on your production database before rollout. The module does not automatically register a Flyway migration location or alter the host's Hibernate configuration.

For a fresh local development database, `initialize-schema: true` runs the bundled idempotent V1 DDL at startup. Keep it false for production and manage future schema changes through versioned migrations. The identity store requires an initialized schema and a writable datasource; missing schema/module fails startup instead of falling back to unrestricted access. `spring.jpa.admin.enabled=false` also disables the JDBC store autoconfiguration.

When the identity tables contain no users, bootstrap credentials create one active superuser with admin access. On subsequent starts, these properties do not reset that account or create another superuser. Remove the bootstrap password from configuration after provisioning. Existing configured-mode credentials are not automatically imported; explicitly choose the initial database account. The sample app uses an in-memory H2 database, so its database users disappear on shutdown.

## Permission model

Each registered entity exposes `view`, `add`, `change`, and `delete` permissions. Each custom action has a separate `action:<actionId>` permission. Names are exact and case-sensitive; grants have no wildcard or deny syntax.

```java
@Entity
@AdminResource(permissionResource = "catalog.Product")
public class Product { /* ... */ }
```

This produces keys such as `catalog.Product:view` and `catalog.Product:change`. Without an explicit identifier, the fully qualified Java class name is used. Set an explicit stable identifier before moving or renaming a Java class; otherwise migrate existing grants to the new key. Duplicate/invalid explicit identifiers fail startup. Newly registered permissions are unassigned by default. Stale grants do not grant access to another resource.

A user's permissions are the union of all their groups' grants. No groups means no entity access. This version has no direct per-user grants, explicit denies, row-level policies, or field-level permissions.

**View is required alongside every write/action permission**, even if it comes from another group. Entity settings (`readOnly`, `creatable`, `updatable`, `deletable`) remain hard restrictions. Superusers bypass grant assignment, but never those entity restrictions; custom actions are disabled on read-only entities.

Users also have independent `active`, `adminAccess`, and `superuser` flags. Only active users with admin access can sign in. Only superusers can manage users/groups. At least one active superuser with admin access must remain; account changes are serialized using a database row lock to enforce this across concurrent changes and instances.

## Administration UI

In database mode, a superuser sees **Users and groups** in the sidebar:

1. Create a group and select entity permissions, including View.
2. Create an admin user and assign one or more groups.
3. Sign in as that user to see the permitted entities and actions.
4. Edit memberships or group grants to change access.

User editing supports disabling accounts, removing console access, resetting passwords, and inspecting effective assigned permissions. Passwords entered through management APIs must contain at least 12 characters and at most 72 UTF-8 bytes. Password hashes never appear in management responses.

Account status and grants are resolved from the database for authorization. Group revocation applies on subsequent requests without requiring logout. Password resets, username changes, and changes to active/admin-access flags invalidate existing database login sessions through an account version. The browser may show stale capabilities until refresh; backend checks always decide whether an operation is allowed.

## API contract

All endpoints below are relative to `{admin-path}/api/security` and require a signed-in database superuser. Writes also require the existing admin CSRF header. Ordinary admins cannot grant themselves permissions through these endpoints or generic entity CRUD.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/permissions` | Registered permission catalog: key, entity label, operation |
| GET / POST | `/users` | List safe user details / create a user |
| PUT / DELETE | `/users/{id}` | Replace account settings or reset password / delete a user |
| GET / POST | `/groups` | List groups / create a group |
| PUT / DELETE | `/groups/{id}` | Replace group name and grants / delete a group |

User POST/PUT bodies contain `username`, `active`, `adminAccess`, `superuser`, `groupIds`, and an optional `password` (required on create, blank/omitted preserves it on update). All three booleans are required. Group bodies contain `name` and `permissions` (the full desired set). PUT replaces memberships/grants; it is not a partial patch. Successful operations return 200; deletion has an empty body. Invalid input or removal of the last superuser returns 400, missing records 404, and conflicting names/references 409.

Entity metadata is copied per user and includes a `permissions` map. Existing create/update/delete flags and action lists are also reduced to effective capabilities so all existing UI entry points obey the same policy. Shared scanner metadata is never modified.

Unauthenticated protected requests return 401. Permission denials return 403 with `code: ACCESS_DENIED`; CSRF denials return 403 with `code: CSRF_INVALID`. Revoked sessions are reported as unauthenticated by `/auth/session`. Clients must not automatically replay failed writes.

## Indirect access and boundaries

- Counts and activity exclude entities without View permission.
- Related fields, nested record data and lookup results require View on the target entity. Filtering or sorting by an inaccessible relation is denied.
- Creating, changing or deleting owned inline children requires the corresponding child permission. Rejected nested operations roll back the parent transaction. Editing an inline collection in the UI requires child Change permission; otherwise it is displayed read-only.
- Deleting a parent with owned inline children also requires child Delete permission. Bulk selections are authorized before deletion begins.
- Upload tickets bind the user, entity, field and add/change operation. Local receiving and completion recheck ownership and current permissions. Previously issued external-storage URLs remain usable until their provider expiry, but completion and saving the record still require authorization.
- Upload URLs configured as public remain public. These permissions do not turn public attachments into confidential storage.
- CSV export uses readable list data. A reader can copy data; hiding an Export button cannot prevent that.
- Custom actions and computed methods are application code. An action grant authorizes that action's effects; review implementations that access other entities or perform external side effects. Arbitrary application callbacks/cascades beyond declared owned inlines are not an object-level authorization sandbox.

## Extending and upgrading

`AdminIdentityStore` is the persistence adapter contract. Provide your own bean to use another store instead of the JDBC implementation. `AdminAuthorization` is the core authorization contract; a custom `@Primary` implementation can supply entity-grant decisions while the admin authentication boundary stays separate. Management endpoints remain restricted to the built-in identity service's superusers. Custom implementations must preserve default denial and current-account checks.

`@AdminPermission` is deprecated. Its old role arrays are not enforced and are not converted to grants. Keeping them inert avoids changing existing deployments' access unexpectedly. Use stable `@AdminResource(permissionResource = ...)` identifiers and database groups instead.

Existing configured-mode installations retain their account and full entity access within entity restrictions. Earlier upload tickets lack the new owner/operation claims and must be retried after upgrading. Existing authentication/session isolation from host security remains unchanged.

## Verification

Run `mvn package` for backend tests and a packaged sample app. From `jpa-admin-ui`:

```sh
CI=1 E2E_PORT=18092 E2E_CHANNEL=chrome E2E_SECURITY_MODE=database npm run test:e2e -- permissions.spec.ts --retries=0
```

This starts the sample with a fresh in-memory identity database, exercises the user/group UI and checks that existing reader sessions lose revoked permissions. Default browser regressions run in configured mode; `AdminPermissionsTest` covers backend bypass attempts, group union/revocation, password resets, nested writes, upload ownership and superuser protections.
