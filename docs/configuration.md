# Configuration

<p class="jpa-lead">Everything is configured under <code>spring.jpa.admin</code> in <code>application.yml</code> or <code>application.properties</code>. Configured mode requires admin credentials; database mode additionally requires an identity store and initialized schema.</p>

## A complete example

```yaml title="application.yml"
spring:
  jpa:
    admin:
      enabled: true                      # turn the console off entirely with false
      path: /admin                       # URL of the console; the API lives at {path}/api
      title: "Acme Back Office"          # login page, sidebar and browser tab
      logo-url: /images/acme-logo.svg    # optional; falls back to the built-in mark
      primary-color: "#2563eb"           # optional brand accent (#rrggbb)
      export-enabled: true               # show the CSV Export button
      page-size: 25                      # rows per page unless @AdminResource(pageSize) is set
      security:
        mode: configured                 # database for users/groups with an identity store
        username: ${ADMIN_USERNAME:admin}
        password: ${ADMIN_PASSWORD}      # required
        login:
          max-attempts: 5                # failed sign-ins before a lock; 0 disables
          lock-duration: 15m
      storage:                           # only needed for @AdminUpload fields
        default:
          type: local
          directory: ./uploads
          public-path: /uploads
      upload:
        signing-key: ${ADMIN_UPLOAD_SIGNING_KEY:}   # set when running several instances
```

## Property reference

All properties use the `spring.jpa.admin.` prefix. Defaults come from [`JpaAdminProperties`](../jpa-admin-spring-boot-autoconfigure/src/main/java/com/substringtechnologies/jpaadmin/boot/config/JpaAdminProperties.java).

### General

| Property | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enables the console and its API. `false` disables library auto-configuration, including the built-in identity store; it does not drop tables. |
| `path` | `/admin` | URL prefix of the console. The REST API is served at `{path}/api`. It must start with `/` and have no trailing slash (for example `/backoffice` or `/internal/admin`). The prebuilt UI adapts automatically. |
| `title` | `Spring Boot JPA Admin` | Name shown on the login page, in the sidebar and in the browser tab. |
| `logo-url` | *(empty)* | Image shown in the sidebar and on the login page. Absolute URLs and same-site paths both work. A broken image falls back to the built-in mark. |
| `primary-color` | *(empty)* | Brand accent as a `#rrggbb` hex value. Buttons, links, focus rings, active navigation and the dashboard chart use it, and text on it switches between black and white for contrast. Other formats are ignored. |
| `export-enabled` | `true` | Shows the **Export** button on tables. Individual entities can opt out with `@AdminResource(exportable = false)`. |
| `page-size` | `25` | Initial rows per page, 1–500, for entities whose `@AdminResource` doesn't set `pageSize`. The API serves at most 500 rows per request. |

### Security

| Property | Default | Description |
| --- | --- | --- |
| `security.mode` | `configured` | `configured` for the existing account; `database` enables optional [users and groups](permissions.md). |
| `security.initialize-schema` | `false` | Identity module only: detect H2/PostgreSQL/MySQL and initialize the matching V1 schema for local development. Use vendor-specific migrations in production. |
| `security.username` | `admin` | Configured account username, or the first database superuser bootstrap name. |
| `security.password` | *(empty)* | **Required in configured mode or when bootstrapping an empty identity database.** BCrypt-encoded; existing database passwords are not overwritten. |
| `security.login.max-attempts` | `5` | Failed sign-ins for one username from one client address before sign-in is locked. Each address may also fail 4× this across all usernames. `0` turns throttling off. |
| `security.login.lock-duration` | `15m` | How long a lock lasts, and the window in which failures are counted. |

See [Security](security.md) for how sign-in, sessions and CSRF work.

### File storage

| Property | Default | Description |
| --- | --- | --- |
| `storage.<name>.type` | `local` | `local`, `s3`, `imagekit`, or the `type()` of a custom `FileStorage` bean. |
| `storage.<name>.public-url` | *(provider URL)* | Base URL saved for uploaded files, for example a CDN or custom domain. |
| `storage.<name>.directory` | `./uploads` | **local**: folder files are written to. Created at startup. |
| `storage.<name>.public-path` | `/uploads` | **local**: URL path the folder is served from. |
| `storage.<name>.bucket` | | **s3**: bucket name (required). |
| `storage.<name>.region` | `us-east-1` | **s3**: AWS region. |
| `storage.<name>.access-key` / `secret-key` | | **s3**: static credentials. Leave empty to use the default AWS credential chain (environment, profile, instance role). |
| `storage.<name>.endpoint` | | **s3**: custom endpoint for MinIO, Cloudflare R2, DigitalOcean Spaces and other S3-compatible services. |
| `storage.<name>.path-style` | `false` | **s3**: use `endpoint/bucket/key` URLs (usually needed for MinIO). |
| `storage.<name>.public-key` | | **imagekit**: public API key (required). |
| `storage.<name>.private-key` | | **imagekit**: private API key (required; never sent to the browser). |
| `storage.<name>.url-endpoint` | | **imagekit**: your URL endpoint, for example `https://ik.imagekit.io/acme` (required). |
| `storage.<name>.upload-url` | ImageKit upload API | **imagekit**: override the upload endpoint (rarely needed). |
| `storage.<name>.api-url` | ImageKit REST API | **imagekit**: override the API endpoint (rarely needed). |
| `upload.signing-key` | *(random per start)* | Secret that signs upload tickets. Set the same value on every instance behind a load balancer. |

The [File & image uploads](file-uploads.md) guide walks through each provider.

## Credentials from the environment

Never commit real passwords. Read them from environment variables or your platform's secret store:

```yaml
spring:
  jpa:
    admin:
      security:
        username: ${ADMIN_USERNAME:admin}   # falls back to "admin"
        password: ${ADMIN_PASSWORD}         # no fallback: must be provided
```

| Expression | Meaning |
| --- | --- |
| `${ADMIN_USERNAME:admin}` | Uses `ADMIN_USERNAME`, or `admin` if it's not set. |
| `${ADMIN_PASSWORD}` | Uses `ADMIN_PASSWORD`; startup fails if it's missing. |
| `${ADMIN_PASSWORD:admin}` | Falls back to `admin`. Only acceptable for local demos, like the sample app. |

A `.env` file is not loaded automatically by Spring Boot. Set the variables in your shell, IDE run configuration, container or deployment platform.

!!! note "Admin login ≠ database login"
    `spring.jpa.admin.security.*` controls who can sign in to the console. `spring.datasource.username/password` connect to your database. They are unrelated and can coexist in the same file.

## application.properties

```properties
spring.jpa.admin.path=/admin
spring.jpa.admin.title=Acme Back Office
spring.jpa.admin.primary-color=#2563eb
spring.jpa.admin.security.username=${ADMIN_USERNAME:admin}
spring.jpa.admin.security.password=${ADMIN_PASSWORD}
spring.jpa.admin.storage.default.type=local
spring.jpa.admin.storage.default.directory=./uploads
```

## What isn't configurable yet

These come up often and aren't available as properties: per-entity include/exclude lists, a global default page size, and an "audit log" switch. Multiple admin users and groups are available through the optional database mode; see [Permissions](permissions.md). See [Limitations & roadmap](limitations.md).


## Configuration precedence and small details

- Set `mode` and `initialize-schema` under `spring.jpa.admin.security`, not directly under `spring.jpa.admin`.
- Supported mode values are exactly `configured` and `database`; invalid values fail startup.
- Profile files such as `application-identity-demo.yml` apply only when that profile is active. The sample's normal database mode and its optional seeded-account profile are different experiences.
- Command-line arguments and environment/run settings can override YAML. Check IDE active profiles and launch arguments when a restart does not use the expected account or port.
- `ADMIN_PASSWORD` is a variable used by the example placeholder. Without that placeholder, the direct Spring property environment name is `SPRING_JPA_ADMIN_SECURITY_PASSWORD`.
- Quote colors such as `"#2563eb"`; an unquoted `#` starts a YAML comment. Keep one `spring` mapping when merging snippets.
- With `server.servlet.context-path: /shop` and admin `path: /backoffice`, open `/shop/backoffice/`. Do not repeat `/shop` inside the admin path.
- In an initialized identity database, changing bootstrap YAML credentials does not reset stored credentials. If you remove the bootstrap secret, also remove a required `${ADMIN_PASSWORD}` placeholder or change it to an optional `${ADMIN_PASSWORD:}`; otherwise placeholder resolution can still fail.
- `initialize-schema: false` does not make identity read-only; accounts, grants and coordination still need a writable datasource.

See [database setup](database-setup.md) for complete datasource examples and [mode behavior](permissions.md#mode-and-dependency-behavior) for dependency removal.
