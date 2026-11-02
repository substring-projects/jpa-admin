# What's new

<p class="jpa-lead">Release notes for <code>0.2.0</code>, followed by historical 0.1.0 notes. Publication of 0.2.0 is pending.</p>

## 0.2.0 release preparation

These changes describe the current source build, not the 0.1.0 release. [Install from source](installation.md) to try them; use matching module versions.

- Optional `jpa-admin-identity` for dedicated admin users, groups, superusers and enforced entity/action permissions.
- Database detection and migrations for H2/PostgreSQL/MySQL; real-server contracts for PostgreSQL 18 and MySQL 8.4.
- Users and Groups management with permission matrix, View prerequisites, search, account badges and responsive dialogs.
- Stable `@AdminResource(permissionResource = ...)` keys; `@AdminPermission` has been removed; migrate to database groups.
- Admin/host security isolation over the whole admin prefix, preserving host authentication on admin logout; servlet-context-aware UI base paths.
- User-bound upload tickets and server-side checks for relations and owned inline children.
- Default sample now enables database mode; optional `identity-demo` seeds viewer/editor accounts.
- Every annotation option now has an effect: `@AdminWidget(min, max, step, language, previewSize)`, `WidgetType.SLIDER`, `InlineType.STACKED`, `@AdminComputed(format)`, `@AdminSensitive(mask, maskPattern)`, `@AdminAction(icon)`, `@AdminLookup(searchProperty)` and the `page-size` property. `JpaAdminConfigurer` beans customize entities in code.
- Sign-in throttling (`security.login.max-attempts`, `lock-duration`) answers repeated failures with `429`.
- API hardening: pages and batches are capped at 500 records, and lookups at 100 records; unexpected errors return a generic `500` instead of exception messages; sensitive fields can't be sorted, and a form that sends the mask back no longer overwrites the stored secret.

Breaking changes in this build: `@AdminPermission` (never enforced) and `@AdminLookup(quickCreate)` were removed, and `@AdminResource(pageSize)` now defaults to `0`, meaning the global `page-size`. Unexpected server errors are now `500`, and actions that refuse with `IllegalStateException` / `IllegalArgumentException` return `409`, where both used to be `400`.

Before upgrading, preserve permission keys, apply the intended database migration and retry old upload tickets that lack owner/operation claims. Existing configured-mode deployments retain their account model unless explicitly changed. Follow [the practical guide](practical-guide.md), [database setup](database-setup.md) and [custom store contract](custom-identity.md).

## 0.1.0 release notes

!!! warning "Upgrading from v1.0.1"
    This release renames packages and changes a few defaults. Read [Breaking changes](#breaking-changes) before you upgrade.

## Highlights

<div class="grid cards" markdown>

-   :material-view-dashboard-outline: __A redesigned console__

    ---

    Rebuilt with React 19 and shadcn/ui. Every page has a real URL, there's a data-focused dashboard and record pages, and light, dark and system themes. [Tour the console](user-guide.md)

-   :material-filter-variant: __Filters & saved views__

    ---

    Relation filters, number and date ranges, removable filter chips, and named views saved per browser. [Filtering](user-guide.md#filter-records)

-   :material-pencil-box-multiple-outline: __Editing everywhere__

    ---

    Edit-in-place table cells, inline child rows saved with their parent, many-to-many multi-selects, and rich-text and code editors. [Forms](user-guide.md#create-and-edit-records)

-   :material-cloud-upload-outline: __File & image uploads__

    ---

    `@AdminUpload` with Amazon S3, S3-compatible services, ImageKit or local storage, live progress, and content verification. [File uploads](file-uploads.md)

-   :material-alert-circle-check-outline: __Field-level validation__

    ---

    Bean Validation runs before every save. Errors come back as `422` with a `fieldErrors` map and appear next to each field. [API errors](api-reference.md#errors)

-   :material-palette-outline: __Branding & custom path__

    ---

    Set a `logo-url` and `primary-color`, and serve the console from any path, such as `/backoffice`, without rebuilding the UI. [Branding](branding.md)

</div>

## Breaking changes

| Change | What to do |
| --- | --- |
| **Package rename.** `io.github.jpaadmin.*` is now `com.substringtechnologies.jpaadmin.*`. | Update imports, for example `import com.substringtechnologies.jpaadmin.annotations.*;`. |
| **Maven Central coordinates** use `com.substringtechnologies` and version `0.1.0`. | Replace the older JitPack dependency. See [Installation](installation.md); the two distribution channels use different version sequences. |
| **Text filters match substrings.** `?name=pro` now matches "MacBook **Pro**" (case-insensitive) instead of only the exact value. | Use the filter as a "contains" search. Enums, booleans, numbers and relations still match exactly. |
| **`spring.jpa.admin.path` is validated.** It must start with `/`, have no trailing slash, and not be `/`. | Use values like `/admin` or `/internal/admin`. Startup fails with a clear message otherwise. |
| **Primary keys are never changed by updates.** An `id` in a `PUT` body is ignored. | Create a new record if a different ID is needed. |
| **Required enum fields start empty on create forms** instead of preselecting the first value. | Users choose a value explicitly; validation asks for it. |
| **Related records are checked on save.** A relation that points to a missing record now fails validation instead of failing later in the database. | No action; you get a clearer error. |

## New API

- `GET {path}/api/stats` returns record counts per entity.
- `GET {path}/api/stats/timeline?days=30` returns records created per day.
- Filters: `field__gte` / `field__lte` range bounds, filtering a relation by its ID, and substring text search. See [Listing](api-reference.md#listing-records).
- `422 Unprocessable Entity` responses include `fieldErrors`; `409 Conflict` is returned for database constraint violations.
- Inline child rows and many-to-many collections can be sent in create and update bodies. See [Write payloads](api-reference.md#write-payloads).
- File uploads: `POST /uploads/prepare`, `PUT /uploads/local/{token}` and `POST /uploads/complete`. See [Uploads](api-reference.md#uploads).
- The session response now includes `logoUrl`, `primaryColor` and `exportEnabled`.

## New configuration

| Property | Purpose |
| --- | --- |
| `spring.jpa.admin.primary-color` | Brand accent color (`#rrggbb`). |
| `spring.jpa.admin.logo-url` | Now displayed in the sidebar and on the login page. |
| `spring.jpa.admin.export-enabled` | Now controls the CSV **Export** button. |
| `spring.jpa.admin.path` | Now honored by the UI, the API and the security rules. |
| `spring.jpa.admin.storage.<name>.*` | File storages for `@AdminUpload`. |
| `spring.jpa.admin.upload.signing-key` | Shared secret for upload tickets across instances. |

See the full [property reference](configuration.md#property-reference).

## New annotation

`@AdminUpload(storage, folder, accept, maxSize)` turns a `String` field into a file upload. See [File uploads](file-uploads.md).
