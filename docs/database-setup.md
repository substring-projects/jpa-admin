# Identity database setup

This guide covers the optional `jpa-admin-identity` module in the **0.2.0-SNAPSHOT source build**. Build and install the repository first; do not assume these features exist in the documented 0.1.0 release. Keep the starter and identity module on the same version.

## Three separate concepts

| Setting or component | Responsibility |
| --- | --- |
| Host JPA datasource | Stores the application's entities; configured under `spring.datasource`. |
| Admin identity store | Stores dedicated admin accounts, groups and grants. The built-in JDBC store uses the host datasource. |
| Admin security mode | Chooses the configured account or database-backed identity; it does not choose the database vendor. |

An application `users` table is not imported into admin identity. A customer or host JWT user does not automatically become an administrator. Admin accounts live in separate `jpa_admin_*` tables, in the same database by default. There is no built-in `spring.jpa.admin.datasource` property. A separately managed datasource requires a [custom identity store bean](custom-identity.md).

## Dependencies

Keep the regular starter and add:

```xml
<dependency>
    <groupId>com.substringtechnologies</groupId>
    <artifactId>jpa-admin-identity</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

Gradle users can use:

```kotlin
dependencies {
    implementation("com.substringtechnologies:jpa-admin-spring-boot-starter:0.2.0-SNAPSHOT")
    implementation("com.substringtechnologies:jpa-admin-identity:0.2.0-SNAPSHOT")
}
```

Use `mavenLocal()` for locally installed snapshots. The identity module does not package a production JDBC driver. Add the appropriate runtime driver below; versions can be managed by your Spring Boot parent/BOM.

## H2 for disposable local development

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:admin-demo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: ""
  jpa:
    admin:
      security:
        mode: database
        initialize-schema: true
        username: ${ADMIN_BOOTSTRAP_USERNAME:admin}
        password: ${ADMIN_BOOTSTRAP_PASSWORD}
```

The password variable must be supplied. An in-memory database disappears when the process stops, even with `DB_CLOSE_DELAY=-1`. This resets admin users, groups and permissions as well as business records in that database. Uploaded files on disk have a separate lifecycle.

## PostgreSQL

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/shop}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
  jpa:
    admin:
      security:
        mode: database
        initialize-schema: false
        username: ${ADMIN_BOOTSTRAP_USERNAME:admin}
        password: ${ADMIN_BOOTSTRAP_PASSWORD:}
```

Apply `jpa-admin/migrations/V1__admin_identity.sql` before starting. Use a database/schema owned by your application; unqualified identity table names follow the datasource's schema/search path. The bootstrap password may be empty only after at least one account has been provisioned. A fresh database with an empty bootstrap password fails startup.

## MySQL

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

Use the PostgreSQL YAML above with a MySQL URL, such as:

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:mysql://localhost:3306/shop}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
```

Merge this into the same `spring` block; do not paste two duplicate top-level `spring` keys. Configure TLS and database access according to your environment rather than copying local test connection flags into production.

Apply **`jpa-admin/migrations/mysql/V1__admin_identity.sql`**. MySQL 8.0+ is accepted; the real-server contract suite currently verifies MySQL 8.4. The schema sets InnoDB, explicit foreign keys, and `utf8mb4_0900_bin` collation. This preserves transactions and case-sensitive username, group-name and permission-key comparisons. `Admin` and `admin` are distinct accounts; choose a consistent naming convention.

## What is created

| Table | Contents |
| --- | --- |
| `jpa_admin_users` | UUID string ID, unique username, BCrypt password hash, active/admin-access/superuser flags and session version |
| `jpa_admin_groups` | UUID string ID and unique name |
| `jpa_admin_user_groups` | User-to-group memberships |
| `jpa_admin_group_permissions` | Exact permission keys granted to a group |
| `jpa_admin_identity_lock` | One coordination row for serialized account/group changes |

These are JDBC tables, not registered JPA entities. They are managed through **Users and groups**, not generic entity CRUD. The permission catalog itself is generated from entity metadata; there is no permission-catalog table. Table names/prefixes are not configurable in the built-in store.

The store inserts the coordination row when it starts. The datasource therefore needs write access, even when schema initialization is disabled. Do not point it at a read-only replica.

## Schema lifecycle

1. Create the database and datasource credentials through your deployment process.
2. Copy the correct vendor SQL from the identity module into your application's migration system. Give it a version that fits your migration history; do not blindly introduce another `V1` alongside an existing application `V1`.
3. Apply the migration before the identity store is constructed. If your application uses custom migration initialization, explicitly arrange that ordering.
4. Run with `mode: database` and `initialize-schema: false`.
5. Supply bootstrap credentials for the first start, verify the admin account, then remove the bootstrap secret or use an optional placeholder such as `${ADMIN_BOOTSTRAP_PASSWORD:}` for later starts.

`initialize-schema: true` is convenient for local development. It runs idempotent CREATE statements, not a versioned migration engine. It does not upgrade existing columns, change collations, add missing foreign keys to existing tables, or repair an old schema. Hibernate `ddl-auto` does not manage these identity tables.

The library does not automatically add a Flyway migration location. Do not scan every migration resource recursively: that would include both the generic and MySQL variants. Existing H2/PostgreSQL V1 SQL is unchanged; preserve applied migration checksums. If MySQL was previously provisioned with generic SQL, review and migrate its foreign keys, engine and collation explicitly.

## Bootstrap and mode changes

- Bootstrap creates one superuser only when the users table is empty. It creates no permission groups.
- Changing YAML credentials does not reset an existing account. Use a superuser's user-edit dialog to reset a password.
- `configured` → `database`: there is no account import. You explicitly provision the initial database superuser.
- `database` → `configured`: identity data remains, but authentication switches to the single configured account with full access within entity restrictions. Treat this as an access-policy change.
- Removing the identity dependency does not delete tables. In database mode, startup fails unless a custom `AdminIdentityStore` bean is present.
- Disabling the admin with `spring.jpa.admin.enabled=false` does not remove existing tables.

## Supported and verified scope

H2, PostgreSQL and MySQL are the built-in database families. Tests run on H2 and actual PostgreSQL 18/MySQL 8.4, including idempotent initialization, permissions, Unicode, unique constraints, foreign keys, rollback, session version updates and concurrent last-superuser protection. This is not a claim that every PostgreSQL or MySQL version/configuration has been tested.

The host's JPA dialect support is separate from identity-store support. For another vendor, register a [custom identity adapter](custom-identity.md). See [permissions verification](permissions.md#run-the-database-contract-tests) for the database contract command; it must run against a disposable database.
