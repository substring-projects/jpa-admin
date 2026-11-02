# Changelog

All notable changes to this project are documented here. The project follows [Semantic Versioning](https://semver.org/).

## [0.2.0] — release prepared, publication pending

### Added
- Optional `jpa-admin-identity` module: database-backed admin users, groups, superusers and enforced entity and action permissions, with schemas for H2, PostgreSQL and MySQL.
- `@AdminWidget(min, max, step)` number bounds, enforced by the API; `WidgetType.SLIDER`; `language` for the code editor; `previewSize` for image thumbnails.
- `InlineType.STACKED` inline rows, `@AdminComputed(format)`, `@AdminSensitive(mask, maskPattern)`, `@AdminAction(icon)` and `@AdminLookup(searchProperty)`.
- `spring.jpa.admin.page-size` now sets the default rows per page.
- `JpaAdminConfigurer` beans customize entity names, icons, groups and order in code.
- Sign-in throttling: `spring.jpa.admin.security.login.max-attempts` (default 5) and `lock-duration` (default 15m); locked attempts get `429` with `Retry-After`.
- Startup fails with the field name for misconfigured widgets and lookup properties.
- CI builds and tests every module on Java 17, 21 and 25 and runs the browser tests.

### Changed
- Pages and batch operations are capped at 500 records per request; lookups are capped at 100.
- Unexpected server errors return `500` with a generic message and are logged; they used to return `400` with the exception message.
- Actions that throw `IllegalStateException` or `IllegalArgumentException` return `409` with that message.
- `@AdminResource(pageSize)` defaults to `0`, meaning the global `page-size`.

### Removed
- `@AdminPermission`, deprecated and never enforced. Use database groups and `@AdminResource(permissionResource)`.
- `@AdminLookup(quickCreate)`, which never had an effect.
- `roles` from entity metadata.
- JitPack distribution: `jitpack.yml`, the committed UI bundle and the old `v1.0.0` / `v1.0.1` tags. Use the Maven Central coordinates.

### Fixed
- Release sources/documentation classifiers for non-Java UI and starter modules.
- Updated Vite to the patched 6.4 maintenance line; refreshed the locked build dependencies.
- Sensitive values in inline rows could be overwritten with the mask text when the parent was saved.
- Sensitive fields could be used as a sort key, revealing the order of their values.

## [0.1.0]

First Maven Central release. See [What's new](docs/whats-new.md#010-release-notes).
