# Configuration

<p class="jpa-lead">Everything is configured under <code>spring.jpa.admin</code> in <code>application.yml</code> or <code>application.properties</code>. Only the admin password is required.</p>

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
      security:
        username: ${ADMIN_USERNAME:admin}
        password: ${ADMIN_PASSWORD}      # required
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
| `enabled` | `true` | Enables the console and its API. `false` registers nothing. |
| `path` | `/admin` | URL prefix of the console. The REST API is served at `{path}/api`. It must start with `/` and have no trailing slash (for example `/backoffice` or `/internal/admin`). The prebuilt UI adapts automatically. |
| `title` | `Spring Boot JPA Admin` | Name shown on the login page, in the sidebar and in the browser tab. |
| `logo-url` | *(empty)* | Image shown in the sidebar and on the login page. Absolute URLs and same-site paths both work. A broken image falls back to the built-in mark. |
| `primary-color` | *(empty)* | Brand accent as a `#rrggbb` hex value. Buttons, links, focus rings, active navigation and the dashboard chart use it, and text on it switches between black and white for contrast. Other formats are ignored. |
| `export-enabled` | `true` | Shows the **Export** button on tables. Individual entities can opt out with `@AdminResource(exportable = false)`. |
| `page-size` | `25` | <span class="jpa-pill jpa-pill--no">Not used</span> Bound but not applied. Use `@AdminResource(pageSize = …)` per entity. |

### Security

| Property | Default | Description |
| --- | --- | --- |
| `security.username` | `admin` | Username of the single admin account. |
| `security.password` | *(empty)* | **Required.** Startup fails while it's blank. The password is hashed with BCrypt in memory at startup. |

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

These come up often and aren't available as properties: multiple admin accounts or roles, per-entity include/exclude lists, a global default page size, and an "audit log" switch. See [Limitations & roadmap](limitations.md).
