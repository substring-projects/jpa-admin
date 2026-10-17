# Spring Boot JPA Admin

[![JitPack](https://jitpack.io/v/substring-projects/jpa-admin.svg)](https://jitpack.io/#substring-projects/jpa-admin)

A Spring Boot starter that discovers JPA entities and provides an administration UI for browsing records, editing scalar fields and single-value relationships, and running custom actions. The React frontend is packaged inside a JAR and served by the Spring application at `/admin/`.

The project is currently **`1.0.0-SNAPSHOT`**. This README describes the implementation in this repository. [PROJECT_REQUIREMENTS.md](PROJECT_REQUIREMENTS.md) describes the broader product roadmap, including features that are not implemented yet.

## Contents

- [Features and status](#features-and-status)
- [Requirements](#requirements)
- [Quick start](#quick-start)
- [Use the starter in another application](#use-the-starter-in-another-application)
- [Configuration](#configuration)
- [Login and security](#login-and-security)
- [Using the dashboard](#using-the-dashboard)
- [Entity customization](#entity-customization)
- [REST API](#rest-api)
- [Project structure](#project-structure)
- [Frontend development](#frontend-development)
- [Build and test](#build-and-test)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)
- [Known limitations and roadmap](#known-limitations-and-roadmap)
- [Contributing](#contributing)

## Features and status

| Area             | Current behavior                                                                                                        |
| ---------------- | ----------------------------------------------------------------------------------------------------------------------- |
| Entity discovery | Inspects the JPA metamodel and generates entity, field, relationship, and action metadata.                              |
| CRUD             | Lists, creates, updates, and deletes records; supports batch deletion.                                                  |
| Tables           | Server-side pagination, sorting, text search, and exact-value scalar filters.                                           |
| Forms            | Metadata-driven inputs, enum selects, boolean switches, date/time inputs, textareas, and common validation attributes.  |
| Relationships    | Searchable selectors for many-to-one and one-to-one references. Annotated child collections are displayed read-only.    |
| Actions          | Instance methods and static methods accepting a `List`, with row/bulk controls and optional confirmation.               |
| UI               | React, TypeScript, Tailwind CSS 4, local shadcn-style components, Radix primitives, and Lucide icons.                   |
| Layout           | Searchable grouped sidebar, desktop collapse, mobile navigation Sheet, light/dark themes, skeletons, and notifications. |
| Authentication   | Properties-based admin credentials, session login/logout, API authorization, and CSRF protection.                       |
| Packaging        | Frontend assets embedded in the UI JAR; no Node.js server required at runtime.                                          |

Exports, editable inline collections, granular role permissions, and several advanced widgets remain incomplete. See [known limitations](#known-limitations-and-roadmap) before integrating the starter into an application.

## Requirements

| Tool     | Requirement                                                                                          |
| -------- | ---------------------------------------------------------------------------------------------------- |
| Java     | JDK 17 or newer; the Maven compiler targets Java 17.                                                 |
| Maven    | Maven 3.9+ for the commands below. No Maven wrapper is included.                                     |
| Node.js  | Node.js 20+ for building the frontend.                                                               |
| npm      | Required on `PATH` when building the Maven reactor.                                                  |
| Database | The sample uses H2 in memory. Host applications supply their own JPA datasource and database driver. |

The current Maven parent is **Spring Boot 4.0.0** and uses its managed Spring Security dependencies. The PRD mentions Spring Boot 3, but this checkout is not a verified Boot 3 compatibility release.

Node.js and npm are build-time requirements only. The current Maven configuration invokes the system `npm`; it does not install Node.js or run `npm ci` automatically.

## Quick start

Run these commands from the repository root:

```bash
# Install the locked frontend dependencies.
npm ci --prefix jpa-admin-ui

# Run frontend regression tests.
npm test --prefix jpa-admin-ui

# Compile modules, run Java tests, build the frontend, and package the sample.
mvn clean package

# Start the sample application.
java -jar jpa-admin-sample-app/target/jpa-admin-sample-app-1.0.0-SNAPSHOT.jar
```

Open **http://localhost:8080/admin/**.

The sample credentials are:

| Field    | Value   |
| -------- | ------- |
| Username | `admin` |
| Password | `admin` |

These defaults are declared in [the sample application.yml](jpa-admin-sample-app/src/main/resources/application.yml), not in frontend code. Replace them for any deployment beyond a local demo.

To override the sample credentials, set environment variables before starting it:

```bash
export ADMIN_USERNAME=store-admin
# Set ADMIN_PASSWORD through your shell or deployment secret configuration.
java -jar jpa-admin-sample-app/target/jpa-admin-sample-app-1.0.0-SNAPSHOT.jar
```

To use another port:

```bash
java -jar jpa-admin-sample-app/target/jpa-admin-sample-app-1.0.0-SNAPSHOT.jar --server.port=8081
```

The sample seeds categories, products, customers, orders, and order items. Its H2 database is in memory: data is recreated when the process restarts. Demo datasource credentials are separate from admin login credentials.

## Use the starter in another application

### Option 1: Via JitPack (Recommended)

You can consume releases directly without building the starter locally.

#### Maven (`pom.xml`)

1. Add the JitPack repository inside your `<repositories>` block:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

2. Add the starter dependency:
```xml
<dependency>
    <groupId>com.github.substring-projects</groupId>
    <artifactId>jpa-admin-spring-boot-starter</artifactId>
    <version>v1.0.0</version>
</dependency>
```

#### Gradle (Groovy - `build.gradle`)

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.substring-projects:jpa-admin-spring-boot-starter:v1.0.0'
}
```

#### Gradle (Kotlin DSL - `build.gradle.kts`)

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.substring-projects:jpa-admin-spring-boot-starter:v1.0.0")
}
```

---

### Option 2: Via Local Installation (`~/.m2`)

If you are developing locally or customizing the library:

1. Build and install into your local Maven cache:
```bash
./mvnw clean install -DskipTests
```

2. In your consuming project:

**Maven:**
```xml
<dependency>
    <groupId>io.github.substring-projects</groupId>
    <artifactId>jpa-admin-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Gradle:**
```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'io.github.substring-projects:jpa-admin-spring-boot-starter:1.0.0-SNAPSHOT'
}
```

Your application must have a configured datasource and JPA entities in its entity-scan scope. The starter supplies Web, Data JPA, Validation, and Security dependencies transitively. Configure admin credentials, start the application, and visit `/admin/`.

## Configuration

### application.yml

Put the admin configuration in your Spring Boot application's
`src/main/resources/application.yml`. For the included sample, edit
[jpa-admin-sample-app/src/main/resources/application.yml](jpa-admin-sample-app/src/main/resources/application.yml).

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

All properties below use the `spring.jpa.admin.` prefix. Defaults come from [JpaAdminProperties](jpa-admin-spring-boot-autoconfigure/src/main/java/io/github/jpaadmin/boot/config/JpaAdminProperties.java).

| Property            | Default                 | Current behavior                                                                                                                                                 |
| ------------------- | ----------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `enabled`           | `true`                  | Enables the JPA admin auto-configuration. Disabling it does not remove transitive Spring Security dependencies or define the host application's security policy. |
| `path`              | `/admin`                | Configures static resource mappings. Keep `/admin`: frontend asset URLs, API URLs, and the security matcher are currently fixed to this prefix.                  |
| `title`             | `Spring Boot JPA Admin` | Displayed on login, in the sidebar, and in the browser title.                                                                                                    |
| `security.username` | `admin`                 | Username for the single configured admin account.                                                                                                                |
| `security.password` | Empty                   | Required, non-blank password. The sample supplies its own `admin` fallback.                                                                                      |
| `page-size`         | `25`                    | Bound property, but not wired into entity metadata or the list controller's default. Use `@AdminResource(pageSize = ...)`.                                       |
| `logo-url`          | Empty                   | Bound property; custom logo rendering is not implemented.                                                                                                        |
| `export-enabled`    | `true`                  | Bound property; export endpoints and UI controls are not implemented.                                                                                            |

Changing credentials requires restarting the application. `security.enabled`, `security.roles`, entity include/exclude patterns, and `default-page-size` from the PRD are not implemented configuration options.

## Login and security

1. The UI requests `/admin/api/auth/session` to check the session and obtain a CSRF token.
2. The login form submits the username and password to the server.
3. Spring Security checks the configured account using BCrypt password encoding and establishes a session.
4. The browser uses the session cookie for subsequent requests. Credentials are not stored in browser local storage.
5. Sign out invalidates the session. An expired session returns the UI to login.

The UI shell and static assets are public so that the login screen can load. Entity metadata, records, lookups, and actions require `ROLE_JPA_ADMIN`. CSRF protection applies to login, logout, and modifying API requests. The frontend refreshes its CSRF token after authentication.

### Existing Spring Security applications

The starter registers a dedicated security chain with order `-100` for `/admin/api/**` and a local authentication manager for the properties-based account. This is not an integration with your application's existing user database, SSO, or role assignments.

Ensure an earlier host security chain does not intercept the admin API. Host applications must explicitly protect their own routes: the starter's admin chain does not secure endpoints outside `/admin/api/**`. Its presence can cause Spring Boot's default catch-all chain to back off.

`@AdminPermission` is currently metadata only; its per-operation role arrays are **not enforced**. Field visibility, field `readOnly`, and custom-action scope are also not complete server-side authorization boundaries. Use the current admin console only for trusted administrators.

The demo H2 console configuration is unrelated to admin authentication and is not protected by the admin API chain.

## Using the dashboard

- **Navigation:** Search entities by name or group. Use the footer control to collapse the desktop sidebar, or the menu button to open mobile navigation.
- **Tables:** Search text, click sortable headers, apply filters, and select a page size. Wide tables scroll horizontally on small screens.
- **Selection:** Select rows for batch deletion or applicable custom actions. Destructive operations use confirmation dialogs.
- **Create/edit:** Forms are generated from metadata. Required fields, email inputs, numeric limits, and text lengths use browser validation where supported.
- **Relations:** Search a related entity, then choose a result. Existing selections remain visible even when outside the current lookup results.
- **Sensitive fields:** Leave a sensitive field blank during editing to retain its current value. The frontend omits unchanged sensitive values from updates.
- **Related collections:** Inline rows are read-only. Manage child records through their own entity lists.
- **Feedback:** Skeletons appear during loading. Save and action controls indicate pending requests; notifications report success or failure.
- **Theme/account:** Toggle light/dark mode in the header and sign out from the sidebar footer.

The dashboard currently summarizes discovered entities and groups, not database record totals or an activity feed.

## Entity customization

Annotations live in `io.github.jpaadmin.annotations`. Prefer field-based JPA mappings with annotations on fields for the current scanner. Working examples are available in [the sample domain package](jpa-admin-sample-app/src/main/java/com/example/demo/domain).

### Resource and field presentation

```java
import io.github.jpaadmin.annotations.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@AdminResource(
    displayName = "Product",
    pluralName = "Products",
    group = "Catalog",
    icon = "package",
    order = 10,
    pageSize = 25,
    defaultSortBy = "id",
    defaultSortDir = SortDir.DESC
)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @AdminField(label = "Product title", order = 1, searchable = true)
    private String name;

    @NotNull
    @Positive
    @AdminField(label = "Price", order = 2, filterable = true)
    private BigDecimal price;

    protected Product() {}
    // Add the accessors and business methods needed by your application.
}
```

| Annotation                  | Main options                                                                                                                       | Notes                                                                                                                              |
| --------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `@AdminResource`            | `displayName`, `pluralName`, `group`, `icon`, `order`, `pageSize`, `defaultSortBy`, `defaultSortDir`                               | Controls entity presentation and defaults.                                                                                         |
| `@AdminResource`            | `hidden`, `readOnly`, `creatable`, `updatable`, `deletable`                                                                        | `hidden` skips discovery. Entity-level CRUD flags are checked by the CRUD service; custom actions have separate behavior.          |
| `@AdminField`               | `label`, `description`, `placeholder`, `order`, `inList`, `inForm`, `inDetail`, `readOnly`, `searchable`, `sortable`, `filterable` | Controls metadata and UI. There is no standalone detail-view screen yet.                                                           |
| `@AdminWidget`              | `type`                                                                                                                             | Implemented special controls include textarea, password, and boolean switch. `RICH_TEXT` currently renders a plain textarea.       |
| `@AdminBadge` / `@BadgeMap` | `value`, `color`, `label`                                                                                                          | Maps enum/string values to table badges.                                                                                           |
| `@AdminSensitive`           | Marker annotation                                                                                                                  | Masks non-null API values and hides the field from the list by default. Names containing `password` or `secret` are also detected. |
| `@AdminLookup`              | `lookupProperty`, `searchProperty`, `quickCreate`                                                                                  | Single-value relations are supported; some options remain incomplete, as explained below.                                          |
| `@AdminInline`              | `type`, `title`, `canAdd`, `canDelete`, `limit`                                                                                    | Discovers related collections; UI rendering is currently read-only.                                                                |
| `@AdminComputed`            | `label`, `order`, `inList`, `inDetail`, `format`                                                                                   | Use conventional `get...` / `is...` getters. Custom formatting is not implemented.                                                 |
| `@AdminAction`              | `name`, `icon`, `confirmation`, `scope`                                                                                            | Exposes supported entity methods as actions.                                                                                       |
| `@AdminPermission`          | `roles`, `createRoles`, `readRoles`, `updateRoles`, `deleteRoles`                                                                  | Declared API; granular authorization is not implemented.                                                                           |

For complete option defaults, see [the annotation sources](jpa-admin-core/src/main/java/io/github/jpaadmin/annotations).

### Status badges

```java
@Enumerated(EnumType.STRING)
@AdminField(label = "Status", filterable = true)
@AdminBadge(mapping = {
    @BadgeMap(value = "DRAFT", color = BadgeColor.GRAY, label = "Draft"),
    @BadgeMap(value = "PUBLISHED", color = BadgeColor.GREEN, label = "Published")
})
private ProductStatus status;
```

The enum values must exist in your application's `ProductStatus` enum.

### Single-value relationships

```java
@ManyToOne(fetch = FetchType.LAZY)
@AdminField(label = "Category")
@AdminLookup(lookupProperty = "name")
private Category category;
```

The record serializer uses `lookupProperty` for a related record's label. The lookup search endpoint currently chooses the target entity's first non-sensitive string field; it does not honor a relation-specific `searchProperty`. The UI requests up to 25 matches and supports searching, but not paged lookup results or quick-create dialogs.

### Sensitive fields

```java
@AdminSensitive
@AdminField(inList = false)
private String apiKey;
```

Sensitive values are returned as a mask rather than their original contents. This annotation does not encrypt or hash stored entity values. Admin account password handling and domain entity password handling are separate concerns.

### Read-only inline collections

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
@AdminInline(title = "Order items", type = InlineType.TABULAR)
private java.util.List<OrderItem> items = new java.util.ArrayList<>();
```

The current edit form displays these related records. It does not implement adding, editing, or deleting child rows within the parent form, regardless of the annotation's `canAdd` or `canDelete` options.

### Computed values

```java
@Transient
@AdminComputed(label = "In stock?", order = 9)
public boolean isInStock() {
    return stockQuantity != null && stockQuantity > 0;
}
```

Computed values are not persisted or writable. The serializer resolves standard getter names; arbitrary annotated method names and format strings are not fully supported.

### Custom actions

```java
@AdminAction(
    name = "Publish product",
    icon = "check-circle",
    confirmation = "Publish the selected products?",
    scope = ActionScope.BOTH
)
public void publish() {
    this.status = ProductStatus.PUBLISHED;
}
```

An instance no-argument method is invoked for each selected record. A static method taking a single `List` is also supported. Extra Spring service parameters are not injected. Available scopes are `SINGLE_ROW`, `BULK_SELECTION`, and `BOTH`; the UI uses them to place action controls.

Action IDs are returned in metadata. Clients should use those IDs rather than deriving them from display names. Actions must enforce their own business rules; resource read-only flags and UI action scope do not provide complete action authorization.

### Validation

The scanner recognizes common constraints including `@NotNull`, `@NotBlank`, `@NotEmpty`, `@Size`, `@Min`, `@Max`, `@Positive`, `@Email`, and `@Pattern`, plus some `@Column` attributes. The form uses required, length, numeric, and email attributes where applicable.

Validation is not an exact frontend replica of Bean Validation. Pattern metadata is not applied by the form, and `@Positive` is currently represented with a `0.01` minimum. Persistence-time validation remains authoritative; errors are returned as general messages rather than a structured field-error map.

## REST API

Base path: **`/admin/api`**. Entity names come from the metadata response and are looked up case-insensitively.

### Authentication endpoints

| Method | Path            | Behavior                                                                                                                                |
| ------ | --------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| `GET`  | `/auth/session` | Public session status, username when authenticated, configured title, CSRF token, and CSRF header name.                                 |
| `POST` | `/auth/login`   | Form-encoded `username` and `password`, with CSRF header and session cookie. Returns `204` on success or `401` for invalid credentials. |
| `POST` | `/auth/logout`  | Requires CSRF verification; invalidates the session and returns `204`.                                                                  |

For a non-browser client, retain cookies from the first session request, send the returned token under the returned `csrfHeader`, and fetch a fresh token after login. The token is tied to the session. Do not send JSON to the login endpoint.

### Entity endpoints

All require an authenticated admin session. Modifying requests also require CSRF verification.

| Method   | Path                           | Request / response                                                  |
| -------- | ------------------------------ | ------------------------------------------------------------------- |
| `GET`    | `/metadata`                    | Entity schemas, fields, relationships, and actions.                 |
| `GET`    | `/{entity}`                    | Paginated records with search, sort, and scalar filters.            |
| `GET`    | `/{entity}/{id}`               | One record, including supported inline data.                        |
| `POST`   | `/{entity}`                    | JSON field map; returns the created record with `201`.              |
| `PUT`    | `/{entity}/{id}`               | JSON field map; updates supplied fields and returns the record.     |
| `DELETE` | `/{entity}/{id}`               | Deletes a record; returns `204`.                                    |
| `POST`   | `/{entity}/batch-delete`       | JSON ID array, such as `[1, 2]`; returns `{"deleted": 2}`.          |
| `GET`    | `/{entity}/lookup`             | `q` and `limit`; returns an array of `{ "id": ..., "label": ... }`. |
| `POST`   | `/{entity}/actions/{actionId}` | JSON ID array; returns `success` and `result`.                      |

There are no implemented `PATCH`, CSV export, or XLSX export endpoints.

### Listing example

```http
GET /admin/api/Product?page=0&size=25&sort=name,asc&q=coffee&status=PUBLISHED
```

| Parameter         | Meaning                                                                                                                       |
| ----------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| `page`            | Zero-based page index; default `0`.                                                                                           |
| `size`            | Page size; controller default `25`. The UI sends its selected page size.                                                      |
| `sort`            | One property and direction, for example `name,asc` or `id,desc`.                                                              |
| `q`               | Case-insensitive substring search across designated text fields. Falls back to eligible text fields when none are designated. |
| `search`          | Alias used when `q` is absent or blank.                                                                                       |
| Other field names | Exact-value filters on recognized scalar properties. Range and multi-value filtering are not implemented.                     |

Example response shape:

```json
{
  "content": [{ "id": 1, "name": "Coffee", "status": "PUBLISHED" }],
  "totalElements": 1,
  "pageNumber": 0,
  "pageSize": 25,
  "totalPages": 1
}
```

Actual record fields depend on your entity metadata.

### Write payloads

Example product payload:

```json
{
  "name": "Coffee",
  "price": 12.5,
  "stockQuantity": 10,
  "status": "DRAFT",
  "category": { "id": 1 }
}
```

Use JSON with `Content-Type: application/json`. A single-value relation accepts an ID or an object containing `id`. Send `null` to clear an optional field. `PUT` binds supplied fields; omit sensitive fields to preserve them. Do not echo masked password values back to the API.

The frontend excludes generated IDs, computed/read-only fields, and unsupported nested fields from its payloads. Direct API clients must not assume these UI restrictions are all enforced by the binder.

### Errors

| Status | Common meaning                                                                                  |
| ------ | ----------------------------------------------------------------------------------------------- |
| `400`  | Binding, validation, unsupported-operation, or action failure handled by the entity controller. |
| `401`  | Missing/expired login or invalid credentials.                                                   |
| `403`  | Missing/invalid CSRF token or insufficient authority.                                           |
| `404`  | Entity or record not found where the service raises a not-found exception.                      |

Entity-controller errors generally use `{"error": "message"}`. Security errors may use a different response shape. See [AdminApiController](jpa-admin-core/src/main/java/io/github/jpaadmin/web/AdminApiController.java) for the exact mappings.

## Project structure

```text
admin-for-springboot/
├── pom.xml                             # Parent and Maven reactor
├── jpa-admin-core/                     # Annotations, metadata scanner, CRUD service, API
├── jpa-admin-ui/                       # React application and static-resource JAR
├── jpa-admin-spring-boot-autoconfigure/ # Properties, MVC resources, session security
├── jpa-admin-spring-boot-starter/       # Consumer dependency aggregator
├── jpa-admin-sample-app/               # H2 demo and annotated entities
├── PROJECT_REQUIREMENTS.md            # Product vision and implementation roadmap
└── README.md
```

At startup, auto-configuration creates the metadata scanner and CRUD service. The scanner inspects managed JPA entities. The browser loads the packaged UI, authenticates, fetches metadata, and generates tables and forms that call the generic API.

Frontend assets are copied from `jpa-admin-ui/dist/` to `META-INF/resources/admin/` in the UI JAR during Maven's `process-resources` phase.

## Frontend development

Run the sample backend on port **8080**, then start Vite in a separate terminal:

```bash
cd jpa-admin-ui
npm ci
npm run dev
```

Open **http://localhost:5173/admin/**. Vite proxies `/admin/api` to `http://localhost:8080`, including login requests. Access the UI and its API through the same browser origin so the session cookie and CSRF token stay together.

If the backend uses another port, update the proxy target in [vite.config.ts](jpa-admin-ui/vite.config.ts). `npm run preview` previews built assets; it is not a replacement for running the backend and configuring API access.

### UI source map

| Location                              | Purpose                                                     |
| ------------------------------------- | ----------------------------------------------------------- |
| `src/views/LoginView.tsx`             | Login form, pending state, and errors.                      |
| `src/context/AuthContext.tsx`         | Session bootstrap and authentication gate.                  |
| `src/api/auth.ts`                     | Login/logout, CSRF token handling, and authenticated fetch. |
| `src/context/AdminContext.tsx`        | Metadata, navigation, theme, and notifications.             |
| `src/components/Sidebar.tsx`          | Grouped navigation, search, collapse, and account footer.   |
| `src/views/EntityListView.tsx`        | Tables, filters, pagination, selection, and actions.        |
| `src/views/EntityFormView.tsx`        | Metadata-driven create/edit forms.                          |
| `src/components/RelationSelect.tsx`   | Async relation search and selection.                        |
| `src/components/LoadingSkeletons.tsx` | Dashboard, form, and table loading placeholders.            |
| `src/components/ui/`                  | Shared local UI primitives.                                 |
| `src/index.css`                       | Tailwind CSS 4 theme tokens, colors, and animations.        |
| `src/lib/form-data.ts`                | Writable-field selection and payload construction.          |

The `@/` alias resolves to `src/`. [components.json](jpa-admin-ui/components.json) configures the shadcn CLI. Tailwind runs through the Vite plugin; the older `tailwind.config.js` is not the active source of theme tokens. Review generated component changes before replacing customized primitives.

## Build and test

From the repository root:

```bash
# Frontend regression tests.
npm test --prefix jpa-admin-ui

# TypeScript check and production frontend bundle.
npm run build --prefix jpa-admin-ui

# Java tests across the reactor; also invokes the frontend build.
mvn test

# Package all modules and the runnable sample JAR.
mvn package

# Install artifacts into the local Maven repository.
mvn install
```

Focused Maven checks:

```bash
# Core and authentication module tests.
mvn -pl jpa-admin-spring-boot-autoconfigure -am test

# Build the UI resource JAR.
mvn -pl jpa-admin-ui -am package
```

Current automated coverage includes:

- Preserving unchanged sensitive values and excluding protected fields from frontend payloads.
- Preserving `0`, `false`, explicit `null`, and manually assigned create IDs.
- Excluding unsupported nested fields from scalar forms.
- Blocking anonymous metadata/record requests.
- Authenticating with configured credentials and invalidating sessions on logout.
- Requiring CSRF tokens for login and authenticated writes.

These are focused regression tests, not a complete end-to-end or cross-browser test suite. There is no dedicated lint script or CI workflow in this checkout. Vite currently reports a large JavaScript bundle warning; the build still completes.

## Deployment

Build the artifacts, then run the host application's JAR with a configured database and admin credentials. Node.js is not required on the runtime host because the UI is served as static resources.

For a deployment beyond the demo:

- Supply a unique admin password through environment or secret configuration and serve the application over HTTPS.
- Replace the sample H2 datasource with your persistent database configuration; manage schema changes through your application's migration process.
- Disable the sample H2 console and define access controls for all non-admin routes.
- Keep the `/admin` prefix and a root application context until custom-path support is completed across the frontend, API, and security configuration.
- Account for session affinity or shared session storage if running multiple instances; distributed sessions are not configured by this starter.
- Review the documented authorization and payload-binding limitations before exposing business entities.

Do not rebuild an executable JAR in place while it is running. Stop the process first or run a separate copy of the artifact, then restart with the new build.

## Troubleshooting

| Problem                                      | What to check                                                                                                                                               |
| -------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Startup asks for admin credentials           | Set `spring.jpa.admin.security.password` to a non-blank value. Set `ADMIN_PASSWORD` when using the documented placeholder.                                  |
| Default login does not work                  | `admin/admin` is only the sample fallback. Environment overrides or your host application's properties take precedence. Restart after changing credentials. |
| UI says it cannot connect                    | Confirm the backend is running. In development, check Vite's port-8080 API proxy. Rebuild/restart an older backend that lacks the session endpoint.         |
| API returns `401`                            | Sign in and retain the session cookie. Refreshing with an expired session requires another login.                                                           |
| API returns `403`                            | Fetch a fresh session/CSRF token, keep its cookie, and send the token header on modifying requests. Check host filter-chain ordering and authorities.       |
| `npm` or `tsc` is missing during Maven build | Install Node/npm and run `npm ci --prefix jpa-admin-ui` before Maven.                                                                                       |
| An entity is missing                         | Check entity scanning and datasource configuration, and ensure `@AdminResource(hidden = true)` is not set. Restart after entity changes.                    |
| No search results or unexpected filters      | Search operates on eligible string fields; filters are exact-value scalar comparisons. Relation and range filters are incomplete.                           |
| Custom URL prefix fails                      | Keep `spring.jpa.admin.path=/admin`; API and asset paths are not fully configurable yet.                                                                    |
| UI changes do not appear in the JAR          | Run `mvn package` and restart using the newly packaged sample/host artifact. Use Vite for live frontend work.                                               |
| Port 8080 is busy                            | Stop the process you own or start the sample with `--server.port=8081`; update the Vite proxy if needed.                                                    |
| Sample records disappear after restart       | Expected for the demo's in-memory H2 database. Configure a persistent datasource to retain data.                                                            |
| An action method fails                       | Use an instance no-argument method or a static method taking one `List`; additional injected parameters are unsupported.                                    |
| An inline row cannot be edited               | Inline collections are currently read-only. Use the child entity's own table and form.                                                                      |

## Known limitations and roadmap

The following are incomplete or not implemented in the current snapshot:

| Area           | Limitation                                                                                                                   |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| Authorization  | Per-entity/per-operation role enforcement, action scope enforcement, and comprehensive server-side field write restrictions. |
| Authentication | Multiple admin accounts, account management, password reset, SSO integration, and login rate limiting.                       |
| Nested data    | Editable inline children, embedded-object forms, collection editors, and full composite-key support.                         |
| Relationships  | Quick create, paged lookup results, relation-specific lookup search properties, and robust foreign-key filters.              |
| Filtering      | Date/numeric ranges and multi-value enum filters.                                                                            |
| Validation     | Structured field-level server errors and complete parity between frontend validation and Bean Validation.                    |
| Widgets        | Rich-text editing, code editors, sliders, file uploads, color pickers, and image previews.                                   |
| Data views     | Record preview drawer, column visibility controls, row density controls, record-count badges, and recent activity.           |
| Export         | CSV/XLSX endpoints and corresponding UI controls.                                                                            |
| Configuration  | Custom deployment prefixes/context paths, entity include/exclude configuration, custom logos, and global page-size wiring.   |
| Extension API  | `JpaAdminConfigurer` and `AdminRegistry` exist but are not invoked by auto-configuration.                                    |
| IDs/types      | Common single IDs are supported; do not assume every JPA ID, temporal type, or mapping style is covered.                     |
| Distribution   | Published release coordinates, automated publishing, and comprehensive CI/browser coverage are not established here.         |

See [PROJECT_REQUIREMENTS.md](PROJECT_REQUIREMENTS.md) for intended behavior and future milestones. Its checklist is a roadmap, not a reliable inventory of completed functionality.

## Contributing

1. Set up and run the sample application.
2. Make changes in the appropriate Maven module or frontend component.
3. Add focused regression coverage when changing data binding, authentication, or other behavior with meaningful failure risks.
4. Run the relevant tests and production build.
5. Check desktop/mobile behavior for UI changes and update this README if public behavior or configuration changes.

No license file is currently included in this repository. Confirm licensing terms with the project owner before redistribution.
