# Configuration

[Documentation index](README.md) · [Project overview](../README.md)

Applies to `1.0.0-SNAPSHOT` (Spring Boot 4.0.0).

### application.yml

Put the admin configuration in your Spring Boot application's
`src/main/resources/application.yml`. For the included sample, edit
[jpa-admin-sample-app/src/main/resources/application.yml](../jpa-admin-sample-app/src/main/resources/application.yml).

If your file already has `spring:` and `jpa:` sections, merge the `admin:` block
into them. Use spaces for YAML indentation and avoid duplicate `spring:` keys.
Admin login credentials belong under **`spring.jpa.admin.security`**, not under
`spring.datasource` or `spring.security.user`.

#### Option 1: Set credentials directly for local development

```yaml
server:
  port: 8080

spring:
  jpa:
    admin:
      enabled: true
      path: /admin
      title: "My Admin Console"
      security:
        username: "admin"
        password: "admin"
```

With this configuration, start the application and open
**http://localhost:8080/admin/**. Enter **admin** in the Username field and
**admin** in the Password field.

To use a different local account, change these two values:

```yaml
security:
  username: "store-admin"
  password: "local-demo-password"
```

This shorter snippet replaces the `security:` block inside `spring.jpa.admin`;
it is not a top-level configuration block. Restart the application after editing
credentials, then sign in with the new values. Quote literal passwords so YAML
characters such as `#`, `:`, or `{` are handled as part of the string.
The values above are demo credentials; use environment variables for deployment
secrets instead of committing real passwords.

#### Option 2: Read credentials from environment variables

```yaml
spring:
  jpa:
    admin:
      enabled: true
      path: /admin
      title: "Store Admin Console"
      security:
        username: "${ADMIN_USERNAME:admin}"
        password: "${ADMIN_PASSWORD}"
```

| YAML value                | Meaning                                                                                            |
| ------------------------- | -------------------------------------------------------------------------------------------------- |
| `${ADMIN_USERNAME:admin}` | Reads `ADMIN_USERNAME`; uses `admin` if the variable is absent.                                    |
| `${ADMIN_PASSWORD}`       | Reads `ADMIN_PASSWORD` with no fallback. You must provide it before startup.                       |
| `${ADMIN_PASSWORD:admin}` | Reads `ADMIN_PASSWORD`; falls back to `admin`. The included sample uses this for local demos only. |

For example, in a local macOS/Linux terminal:

```bash
export ADMIN_USERNAME="store-admin"
export ADMIN_PASSWORD="local-demo-password"
java -jar jpa-admin-sample-app/target/jpa-admin-sample-app-1.0.0-SNAPSHOT.jar
```

For that example, sign in with `store-admin` / `local-demo-password`. In an IDE,
set the same variables in the application's run configuration. In a deployment,
supply them through the platform's environment or secret configuration. A `.env`
file alone is not automatically loaded by this backend.

#### Admin credentials versus database credentials

These settings serve different purposes and can coexist in the same YAML file:

```yaml
spring:
  datasource:
    username: "sa" # Database connection user, not the admin login.
    password: "" # Local H2 database password.
  jpa:
    admin:
      security:
        username: "${ADMIN_USERNAME:admin}" # Admin UI login user.
        password: "${ADMIN_PASSWORD}" # Admin UI login password.
```

Keep your existing datasource URL, driver, and other JPA settings when merging
this snippet. Setting the database username/password does not change the admin
UI login. These credentials configure one admin account; they do not create a
record in your application's customer or user tables.

### application.properties

```properties
spring.jpa.admin.enabled=true
spring.jpa.admin.path=/admin
spring.jpa.admin.title=Store Admin Console
spring.jpa.admin.security.username=${ADMIN_USERNAME:admin}
spring.jpa.admin.security.password=${ADMIN_PASSWORD}
```

The host application must provide `ADMIN_PASSWORD` when using an environment-variable example without a password fallback. Unlike the sample, the starter has **no usable default password** and fails startup when the configured username or password is blank.

### Property reference

All properties below use the `spring.jpa.admin.` prefix. Defaults come from [JpaAdminProperties](../jpa-admin-spring-boot-autoconfigure/src/main/java/com/substringtechnologies/jpaadmin/boot/config/JpaAdminProperties.java).

| Property            | Default                 | Current behavior                                                                                                                                                 |
| ------------------- | ----------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `enabled`           | `true`                  | Enables the JPA admin auto-configuration. Disabling it does not remove transitive Spring Security dependencies or define the host application's security policy. |
| `path` | `/admin` | URL prefix for the UI and its API (`{path}/api`). It must start with `/` and have no trailing slash, for example `/backoffice`. The prebuilt UI adapts automatically, so no rebuild is needed. |
| `title`             | `Spring Boot JPA Admin` | Displayed on login, in the sidebar, and in the browser title.                                                                                                    |
| `security.username` | `admin`                 | Username for the single configured admin account.                                                                                                                |
| `security.password` | Empty                   | Required, non-blank password. The sample supplies its own `admin` fallback.                                                                                      |
| `page-size`         | `25`                    | Bound property, but not wired into entity metadata or the list controller's default. Use `@AdminResource(pageSize = ...)`.                                       |
| `logo-url` | Empty | Image URL shown in the sidebar and on the login page. Falls back to the default mark if it fails to load. |
| `primary-color` | Empty | Brand accent as a `#rrggbb` hex value (for example `#2563eb`). Other formats are ignored and the built-in theme is used. |
| `export-enabled` | `true` | Shows the **Export** button on entity tables, which downloads the current search/filter results (up to 50,000 rows) as CSV. |

| `storage.<name>.*` | none | Named file storages for `@AdminUpload`. See [File storage](#file-storage). |
| `upload.signing-key` | random | Secret that signs upload tickets. Set it when several instances serve the admin behind a load balancer. |

Changing credentials requires restarting the application. `security.enabled`, `security.roles`, entity include/exclude patterns, and `default-page-size` from the PRD are not implemented configuration options.


## File storage

`@AdminUpload` fields store files in named storages. A field that doesn't name one uses `default`, so most applications configure only that. Switching provider is a configuration change; entity code stays the same.

```yaml
spring:
  jpa:
    admin:
      storage:
        default:                 # used by @AdminUpload fields without a storage name
          type: s3
          bucket: shop-images
          region: ap-south-1
          access-key: ${AWS_ACCESS_KEY_ID}
          secret-key: ${AWS_SECRET_ACCESS_KEY}
        avatars:                 # @AdminUpload(storage = "avatars")
          type: imagekit
          public-key: ${IMAGEKIT_PUBLIC_KEY}
          private-key: ${IMAGEKIT_PRIVATE_KEY}
          url-endpoint: https://ik.imagekit.io/your_id
        invoices:                # @AdminUpload(storage = "invoices", accept = "application/pdf")
          type: local
          directory: ./uploads/invoices
          public-path: /files/invoices
```

| Type | Properties | How uploads travel |
| --- | --- | --- |
| `local` | `directory` (default `./uploads`), `public-path` (default `/uploads`) | Browser → admin API → disk. Files are served from `public-path` with `nosniff` and a sandboxing Content-Security-Policy. |
| `s3` | `bucket`, `region`, `access-key`, `secret-key`, optional `endpoint` and `path-style` for MinIO / Cloudflare R2 / Spaces | Browser → presigned PUT straight to the bucket. Without keys the default AWS credential chain is used. Requires the `software.amazon.awssdk:s3` dependency. |
| `imagekit` | `public-key`, `private-key`, `url-endpoint` | Browser → ImageKit's upload API with a short-lived signature; the private key never leaves the server. |

Every type also accepts `public-url`, a CDN or custom domain used for the saved URLs instead of the provider's own.

**S3 CORS.** Because browsers upload directly to the bucket, the bucket needs a CORS rule for the admin's origin:

```json
[{ "AllowedOrigins": ["https://admin.example.com"], "AllowedMethods": ["PUT"], "AllowedHeaders": ["*"], "MaxAgeSeconds": 3000 }]
```

**Custom providers.** Declare a `FileStorage` bean whose `type()` returns a name such as `cloudinary`, and set `type: cloudinary` on a storage.
