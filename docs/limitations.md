# Limitations & roadmap

<p class="jpa-lead">An honest list of what the console does not do yet, so you can decide before you depend on it.</p>

## Access control

- **One admin account** configured in properties. There are no user accounts, password resets or SSO integration.
- **`@AdminPermission` isn't enforced.** Everyone who signs in can read and change every entity the console exposes (within each entity's `readOnly`, `creatable`, `updatable` and `deletable` flags).
- **No audit log** of who changed what.
- **No rate limiting** or lockout on login.

## Data model

- **Embedded objects** (`@Embedded`) are shown but not editable in forms.
- **One-to-many collections without `@AdminInline`** aren't shown.
- **Many-to-many inverse sides** are read-only; record pages list up to 200 linked records.
- **Composite primary keys** aren't supported; single IDs (`Long`, `Integer`, `UUID`, `String`) are.
- **Relation pickers** return the first 25 matches, without paging; `@AdminLookup(searchProperty, quickCreate)` aren't used yet.
- **Multi-value filters** ("status is A or B") and OR conditions aren't available.

## Uploads

- One file per field: no galleries or multi-file fields yet.
- Replacing or removing a file keeps the old file in storage.
- No built-in image resizing or thumbnails.
- S3 and ImageKit are covered by automated tests against simulated services. Try them with a test bucket or account before production.

## Annotation options not used yet

`@AdminWidget(min, max, step, language, previewSize)`, `WidgetType.SLIDER`, `InlineType.STACKED`, `@AdminComputed(format)`, `@AdminSensitive(mask, maskPattern)`, `@AdminAction(icon)` (a standard icon is shown) and the `page-size` property. The [annotation reference](annotations.md) marks each one.

## Platform

- Built and tested with **Spring Boot 4.0**; Spring Boot 3 is not verified.
- **Servlet** applications only (no WebFlux).
- The console `path` can't be the site root (`/`).
- The **Records created** chart needs a creation timestamp field named like `createdAt` (date or date-time).
- `JpaAdminConfigurer` and `AdminRegistry` exist in the codebase but aren't called by auto-configuration.
- English only; no translations yet.
- Saved views, hidden columns, density, theme and recently viewed records are stored per browser and don't sync.
- CSV export only (no XLSX), up to 50,000 rows.

## Roadmap

Ideas under consideration, roughly in order:

1. Image galleries and multi-file uploads, with reordering.
2. Change history for records.
3. Multiple users with roles, enforcing `@AdminPermission`.
4. Quick-create from relation pickers, and paged lookups.
5. Embedded-object editing.

Ideas and feedback are welcome on [GitHub](https://github.com/substring-projects/jpa-admin/issues).
