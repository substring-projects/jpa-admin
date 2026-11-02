# Sample application

The sample includes both `jpa-admin-spring-boot-starter` and the optional `jpa-admin-identity` module. Database users, groups and permissions are enabled by default in `application.yml`. Run normally and open **http://localhost:8080/admin/**; sign in with `admin` / `admin` (unless overridden by `ADMIN_USERNAME` / `ADMIN_PASSWORD`). Open **Users and groups** to create dedicated admin accounts and permission groups. The default sample initializes the identity schema in its in-memory H2 database, so accounts and changes reset when the process stops.

For pre-created viewer/editor accounts, use the optional local demo profile below.

From the repository root:

```sh
mvn package
java -jar jpa-admin-sample-app/target/jpa-admin-sample-app-0.2.0-SNAPSHOT.jar --spring.profiles.active=identity-demo
```

Open **http://localhost:18093/admin/**.

| Username | Demo password | Access |
| --- | --- | --- |
| `admin` | `admin-demo-123` | Superuser: all entities and Users and groups management |
| `viewer` | `viewer-demo-123` | Product Readers: view Products only |
| `editor` | `editor-demo-123` | Catalog Editors: view/add/change Products; view Categories and Tags; no delete or custom actions |

`ADMIN_USERNAME` and `ADMIN_PASSWORD`, if set, override only the bootstrap superuser credentials. The viewer/editor credentials above are intentionally fixed demo credentials. This profile binds to loopback, uses an in-memory H2 database, and initializes the identity schema for local development. Do not use the profile for production. All demo data, accounts and permission changes reset when the process stops.

## Try the permissions

1. Sign in as `admin` and open **Users and groups**. Inspect the two pre-created groups and their grants.
2. In an incognito window, sign in as `viewer`. Only Products is visible and create/edit/delete controls are unavailable.
3. Sign in as `editor` to create/edit a Product and select its Category/Tags. Product deletion and custom actions remain denied.
4. As `admin`, remove Product Change from Catalog Editors. The editor's next write is denied without needing to sign out. Reload to update the visible controls.
5. Restore the grant, disable the user or reset its password to explore the other access controls.

These are dedicated admin accounts in the `jpa_admin_*` tables, independent of any host application's users or JWT login. See [the permissions guide](../docs/permissions.md) for production migrations and the complete API contract.


Normal startup creates only the bootstrap superuser, with no groups. The `identity-demo` profile is required for the seeded viewer/editor accounts above. YAML bootstrap credentials do not reset existing accounts; sample restart creates a new in-memory database, which is why bootstrap runs again.

For your own application, use [the practical setup guide](../docs/practical-guide.md), [database-specific configuration](../docs/database-setup.md), and [custom identity adapters](../docs/custom-identity.md). If a UI change is missing, rebuild the frontend and repackage/restart the sample; see [development instructions](../docs/development.md#seeing-ui-changes-in-the-running-application).
