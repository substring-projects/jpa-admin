# Feature status and limitations

[Documentation index](README.md) · [Project overview](../README.md)

Applies to `1.0.0-SNAPSHOT` (Spring Boot 4.0.0).

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

Exports, editable inline collections, granular role permissions, and several advanced widgets remain incomplete. See [known limitations](limitations.md#known-limitations-and-roadmap) before integrating the starter into an application.

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
| Distribution   | JitPack build configuration exists; release availability and published coordinates are not verified by this documentation. Comprehensive CI/browser coverage is not established.         |

See [PROJECT_REQUIREMENTS.md](../PROJECT_REQUIREMENTS.md) for intended behavior and future milestones. Its checklist is a roadmap, not a reliable inventory of completed functionality.
