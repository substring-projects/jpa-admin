# Troubleshooting

<p class="jpa-lead">Common problems and how to fix them, grouped by when they happen.</p>

## Startup

| Problem | Fix |
| --- | --- |
| `Configure spring.jpa.admin.security.username and spring.jpa.admin.security.password` | Set a non-blank password, for example `password: ${ADMIN_PASSWORD}`, and provide `ADMIN_PASSWORD`. |
| `spring.jpa.admin.path must start with '/', have no trailing slash…` | Use a value like `/admin` or `/backoffice`, not `admin/`, `/admin/` or `/`. |
| `@AdminUpload fields reference storages that aren't configured: Product.imageUrl uses storage "default"` | Add `spring.jpa.admin.storage.default` (or the named storage). See [File uploads](file-uploads.md#storages). |
| `… uses type s3, which needs the dependency software.amazon.awssdk:s3` | Add the AWS SDK `s3` dependency. |
| `spring.jpa.admin.storage.<name>.public-key is required for type imagekit` | Fill in the provider settings listed in the message. |
| `@AdminUpload on X.y requires a String field` | Use a `String` field that stores the file URL. |
| `Invalid @AdminUpload maxSize "5 megs"` | Use `500KB`, `5MB` or `1GB`. |
| `Could not find artifact … in central` | Check the Central coordinates and publication status, then retry with `-U` ([Installation](installation.md#1-add-the-dependency)). |

## Signing in and access

| Problem | Fix |
| --- | --- |
| The console page is blank or 404 | Open `{path}/` with the trailing slash, for example `/admin/`. Check that `spring.jpa.admin.enabled` isn't `false` and that the app is a servlet web application. |
| "Invalid username or password" | In configured mode check effective YAML/environment values and restart. In database mode check the stored account, Active/Admin access flags and password; YAML changes do not reset an existing database password. |
| "Your session has expired" | Sign in again. Behind several instances, use sticky sessions or Spring Session. |
| `403` on saves from scripts | Send the CSRF header from `/auth/session` with the session cookie. Fetch a fresh token after signing in, because the session rotates. |
| Another security chain intercepts the API | The admin chain has order `-100` and matches `{path}` and `{path}/**`. Make sure your own chains don't claim that path first. |

## Data

| Problem | Fix |
| --- | --- |
| An entity is missing | It must be a JPA `@Entity` in your `EntityManagerFactory` (check `@EntityScan`) and not `@AdminResource(hidden = true)`. |
| A field is missing from forms | Check `inForm`; generated IDs, computed values and one-to-many collections without `@AdminInline` aren't in forms. |
| Search finds nothing | Search covers text fields marked `searchable = true`, or all text fields if none are marked. |
| A filter isn't offered | Mark the field `filterable = true`. |
| "Invalid filter value for Price" | The URL has a filter value of the wrong type; remove the chip or fix the value. |
| "This change conflicts with existing data" (`409`) | A unique value is already used, or the record is still referenced by another record. |
| "The selected record no longer exists" | The related record was deleted; choose another. |
| A custom action fails | Use a no-argument instance method or a static method taking one `List`; injected parameters aren't supported. |
| Sample data disappeared | The sample's H2 database is in memory and resets on restart. |

## Uploads

| Problem | Fix |
| --- | --- |
| "Couldn't reach the file storage … (CORS)" | Add a CORS rule to the bucket that allows `PUT` from the admin's origin ([S3 CORS](file-uploads.md#storages)). |
| "The file's content doesn't match its type" | The file isn't what its extension says (for example, a renamed HTML file). Export it in a real image or PDF format. |
| "isn't an allowed file type" or "the limit is 5 MB" | Adjust `accept` / `maxSize` on `@AdminUpload`, or choose another file. |
| "The upload took too long. Please try again." | The 30-minute ticket expired. Upload again. |
| "Invalid upload ticket" (several instances) | Set the same `upload.signing-key` on every instance. |
| Uploaded images don't show | For local storage, check that `public-path` isn't blocked by your own security rules. For S3, check that the objects are publicly readable or set `public-url` to a CDN. |

## UI and builds

| Problem | Fix |
| --- | --- |
| UI changes don't appear in the JAR | Build React **and repackage the sample/app JAR**, then restart and refresh. `npm run build` alone updates `dist`, not the existing sample JAR. See [UI packaging](development.md#seeing-ui-changes-in-the-running-application). |
| `npm` not found during the Maven build | Install Node.js 20+ and run `npm ci --prefix jpa-admin-ui`, then build again. `-DskipNpmBuild=true` only works after a previous local UI build. |
| Saved views or hidden columns are gone | They're stored per browser; clearing site data removes them. |
| Port 8080 is busy | Start with `--server.port=8081`. |


## Identity and permissions

| Symptom | Check and resolution |
| --- | --- |
| `Database admin security requires jpa-admin-identity or an AdminIdentityStore bean` | Keep database mode and add the same-version identity module or your custom store bean. It intentionally does not fall back to configured mode. |
| Unsupported admin identity database | Built-in support is H2/PostgreSQL/MySQL 8.0+. Supply a [custom adapter](custom-identity.md) for another vendor; a host JPA dialect alone is insufficient. |
| Identity table not found at startup | Apply the correct vendor migration before startup, or use `initialize-schema: true` only for local initialization. Check the datasource/schema and migration ordering. Hibernate does not create identity tables. |
| Missing datasource or driver | Configure `spring.datasource` and add the host JDBC driver. Identity's driver dependencies are test-only, not supplied to consumers. |
| YAML says database mode but the old login still appears | Confirm the running artifact, active profiles and command-line overrides. Stop/restart the correct process and use its port. |
| Users and Groups is missing | It requires database mode and a current active/admin-enabled superuser. Configured mode deliberately has no account-management UI. |
| There are no groups | Expected: normal bootstrap creates only one superuser. Create groups manually; only `identity-demo` seeds example groups. |
| Host users are absent from Users | That screen lists dedicated admin identities. Host JPA users are a separate entity if exposed. |
| User logs in but sees no entities | Assign a group with View grants; non-superusers with no groups have no entity access. |
| Add/Change is granted but access is denied | View is also required. Check entity `readOnly`/operation flags and the stable resource key. |
| Permission is visible but a superuser cannot edit | Entity hard restrictions still apply to superusers. The catalog includes standard operations even when an entity disables them. |
| Category/tag selector or relation field disappears | Give View on the related entity. Granting Product Change does not grant Category View. |
| Parent save/delete fails for inline children | Check child View and operation grants, and inline `canAdd`/`canDelete`/`limit`. A denied nested write rolls back the parent. |
| Removing a permission from one group changes nothing | Check other assigned groups and Superuser. Grants are a union; there is no deny rule. |
| Old controls remain visible after revocation | Refresh metadata by reloading. The backend already checks current grants on subsequent requests. |
| Account disappears after sample restart | Sample H2 is in-memory. Use a persistent database for durable users/groups. |
| Bootstrap password was changed but login still uses the old one | Bootstrap runs only for an empty users table. Reset through another superuser instead. |
| Removing bootstrap secret breaks startup despite existing accounts | A required `${...}` placeholder still needs a value. Remove that config entry or use an empty fallback after initial provisioning. |
| Last-superuser error on delete/disable | Keep another active superuser with admin access before removing this one's access. Do not delete tables to bypass it. |
| MySQL initialization succeeds but old tables lack constraints | CREATE IF NOT EXISTS does not update existing tables. Apply an explicit corrective migration; do not rerun or overwrite an applied migration expecting repair. |
| Unknown permission key after a class rename | Use a stable `permissionResource` and migrate old group grants intentionally. The default namespace includes the Java package/class name. |
| Password reset rejected | Management passwords require at least 12 characters and at most 72 UTF-8 bytes; non-ASCII characters may take multiple bytes. |

## Distinguish API failures

- `401`: admin authentication is missing, expired or invalidated. A host bearer token is not an admin session.
- `403` with `CSRF_INVALID`: obtain a fresh token using the same cookie jar. Fetch it again after login/session rotation.
- `403` with `ACCESS_DENIED`: inspect grants, account flags and entity restrictions; a new CSRF token will not grant permission.
- `400`: malformed identity input, unknown permission/group, or a protected business rule such as the last superuser.
- `404`: missing entity record, admin user or group; verify IDs and configured URL prefixes.
- `409`: conflicting names or database references. Correct the data instead of automatically retrying the same write.
- `422` on entity writes: inspect `fieldErrors` for validation failures.

Do not automatically replay failed mutations. First determine whether the operation was accepted and why a later response or request failed.
