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
| "Invalid username or password" | Check the values in effect; environment variables override `application.yml`. Restart after changing them. |
| "Your session has expired" | Sign in again. Behind several instances, use sticky sessions or Spring Session. |
| `403` on saves from scripts | Send the CSRF header from `/auth/session` with the session cookie. Fetch a fresh token after signing in, because the session rotates. |
| Another security chain intercepts the API | The admin chain has order `-100` and matches `{path}/api/**`. Make sure your own chains don't claim that path first. |

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
| UI changes don't appear in the JAR | Rebuild with `./mvnw package` (or `npm run build` in `jpa-admin-ui`) and restart. JitPack builds use the committed `jpa-admin-ui/dist`. |
| `npm` not found during the Maven build | Install Node.js 20+ and run `npm ci --prefix jpa-admin-ui`, or build with `-DskipNpmBuild=true` to reuse the committed `dist`. |
| Saved views or hidden columns are gone | They're stored per browser; clearing site data removes them. |
| Port 8080 is busy | Start with `--server.port=8081`. |
