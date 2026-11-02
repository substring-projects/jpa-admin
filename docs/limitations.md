# Limitations & roadmap

<p class="jpa-lead">An honest list of what the console does not do yet, so you can decide before you depend on it.</p>

## Access control

- **No built-in SSO integration.** Optional [database users and groups](permissions.md) support account management and password resets; configured mode retains one admin account.
- **Entity-level permissions only.** No row-level or field-level grants or explicit deny rules.
- **No audit log** of who changed what.
- **Sign-in throttling is per instance.** Failure counts live in memory, so each instance behind a load balancer counts separately and a restart clears them.

## Data model

- **Embedded objects** (`@Embedded`) are shown but not editable in forms.
- **One-to-many collections without `@AdminInline`** aren't shown.
- **Many-to-many inverse sides** are read-only; record pages list up to 200 linked records.
- **Composite primary keys** aren't supported; single IDs (`Long`, `Integer`, `UUID`, `String`) are.
- **Relation pickers** return the first 25 matches, without paging, and can't create a related record in place.
- **Multi-value filters** ("status is A or B") and OR conditions aren't available.

## Uploads

- One file per field: no galleries or multi-file fields yet.
- Replacing or removing a file keeps the old file in storage.
- No built-in image resizing or thumbnails.
- S3 and ImageKit are covered by automated tests against simulated services. Try them with a test bucket or account before production.

## Annotation options

`WidgetType.DATE_PICKER` and `DATE_TIME_PICKER` behave like `AUTO`, because date fields already get native pickers. Every other annotation option has an effect; the [annotation reference](annotations.md) describes each one.

## Identity administration

- Built-in persistence targets H2, PostgreSQL and MySQL 8.0+; the tested server versions are PostgreSQL 18 and MySQL 8.4. Other vendors need a custom store.
- No built-in forgotten-password email, account invitation, MFA or password-recovery CLI.
- Account/group lists are loaded in full; management pagination is not implemented.
- Dedicated table names are fixed, and the built-in adapter uses the host datasource. Separate storage requires a custom bean.
- Startup schema initialization creates missing tables; it is not an upgrade/migration engine.
- Users and Groups dialogs discard unsaved edits on Close/Cancel without an unsaved-change prompt.

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
3. Row-level access policies and identity-provider integration.
4. Quick-create from relation pickers, and paged lookups.
5. Embedded-object editing.

Ideas and feedback are welcome on [GitHub](https://github.com/substring-projects/jpa-admin/issues).
