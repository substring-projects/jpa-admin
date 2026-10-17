# Spring Boot JPA Admin

A Spring Boot starter that discovers JPA entities and generates an administration console for browsing, creating, updating, and deleting records. The React UI is embedded in a JAR and served by your Spring application at `/admin/`.

[![Documentation](https://img.shields.io/badge/Docs-Online-blue?style=flat-square)](https://substring-projects.github.io/jpa-admin/)
[![Creator](https://img.shields.io/badge/Creator-Durgesh%20Tiwari-orange?style=flat-square&logo=github)](https://github.com/durgeshcoder)

- 📖 **Documentation:** [https://substring-projects.github.io/jpa-admin/](https://substring-projects.github.io/jpa-admin/)
- 👤 **Creator Profile:** [Durgesh Tiwari (@durgeshcoder)](https://github.com/durgeshcoder)

**Version:** `1.0.0-SNAPSHOT` · **Java:** 17+ · **Spring Boot:** 4.0.0

[Documentation](https://substring-projects.github.io/jpa-admin/) · [Website setup](docs/publishing-docs.md) · [Installation](docs/installation.md) · [API reference](docs/api-reference.md) · [Contributing](CONTRIBUTING.md)

## Screenshots

| Dashboard Overview | Entity Table & Search |
| :---: | :---: |
| ![Dashboard](docs/assets/images/dashboard.png) | ![Entity Table](docs/assets/images/entity-table.png) |

| Foreign Key Relations (Selectbox) | Bulk Selection & Deletions |
| :---: | :---: |
| ![Foreign Key Selectbox](docs/assets/images/relation-select.png) | ![Batch Operations](docs/assets/images/batch-operations.png) |

## Key Features & Capabilities

### ⚛️ Smooth Single Page Application (SPA) with React & Tailwind CSS
- **Modern React 19 & Vite Stack:** True Single Page Application architecture delivering instantaneous client-side navigation without full-page reloads.
- **Fluid & Responsive UI:** Crafted with **Tailwind CSS** and **Radix UI** primitives for a polished, modern look across desktop, tablet, and mobile screens (with collapsible drawer navigation).
- **Instant Light & Dark Theme:** Smooth, flicker-free toggling between Slate Light and Onyx Dark modes with persistent user preferences.
- **Micro-Interactions & Feedback:** Skeleton loading placeholders, live toast notifications, and interactive modals for create, edit, view, and delete operations.

### ⚡ Instant JPA Entity Discovery & Full CRUD
- **Zero-Configuration Metamodel Introspection:** Automatically scans and discovers all JPA `@Entity` classes at boot time without boilerplate code.
- **Dynamic Data Grids:** Server-side pagination, adjustable page sizes, multi-column sorting, and debounce-optimized full-text search.
- **Scalar & Column Filtering:** Instant filtering across entity attributes, strings, numbers, dates, and enums.
- **Smart Form Generation:** Type-aware form controls for string inputs, text areas, datetime formatters, toggle switches for booleans, and enum selects.

### 🔗 Intelligent Relationship Management
- **Searchable Foreign Keys:** Automatically resolves `@ManyToOne` and `@OneToOne` associations into asynchronous, searchable select dropdowns.
- **Descriptive Entity Labels:** Displays human-readable labels derived from entity `toString()` or marked `@AdminField` identifier instead of raw database IDs.
- **Inline Collection Views:** Clear, read-only inline representation for `@OneToMany` and `@ManyToMany` collections.

### 🛡️ Built-in Enterprise Security & Session Management
- **Cookie-Backed Authentication:** Dedicated, styled session login portal with configurable administrator credentials.
- **Automated CSRF Protection:** Seamless CSRF token rotation and validation for all state-changing operations (POST, PUT, DELETE).
- **Sensitive Field Masking:** Password and secret fields are automatically masked with reveal toggles and protected against accidental blank overwrites.

### 🎨 Declarative Customization via Annotations
- **`@AdminResource`:** Configure custom display names, entity icons, default sorting rules, read-only mode, and domain groupings.
- **`@AdminField`:** Control field ordering, custom display names, form widgets (textarea, toggle, date picker), and read-only/hidden visibility.
- **`@AdminBadge`:** Map entity status values and enums to clean, color-coded badges (success, warning, destructive, info).
- **`@AdminAction`:** Register custom business actions and workflow operations directly on domain records.

### 🗂️ Batch Operations & Safe Deletions
- **Multi-Record Selection:** Check individual rows or use select-all to perform bulk operations across tables.
- **Safety Confirmation Modals:** Destructive actions prompt explicit confirmation dialogs to prevent accidental data loss.

### 📦 Zero Runtime Node.js Dependency
- **Self-Contained Starter:** Pre-compiled React frontend assets are packaged directly into the Spring Boot starter JAR and served from the classpath at `/admin/`. No Node.js or npm required in production environments!

### 🔌 Headless REST Management API
- **Exposed REST Endpoints:** Complete API exposed under `/admin/api/**` providing metadata introspection (`/admin/api/metadata`, `/admin/api/resources/{resource}/schema`) and CRUD operations for programmatic integration.

This snapshot is intended for trusted administrators. Granular permissions and comprehensive server-side field restrictions are incomplete. Read [security](docs/security.md) and [feature limitations](docs/limitations.md) before exposing application data. CSV/XLSX export and editable inline collections are not implemented.

## Run the sample

Prerequisites: JDK 17+, Node.js 20+, and npm on `PATH`. The included Maven Wrapper downloads Maven 3.9.9 on first use.

```bash
npm ci --prefix jpa-admin-ui
npm test --prefix jpa-admin-ui
./mvnw clean package
java -jar jpa-admin-sample-app/target/jpa-admin-sample-app-1.0.0-SNAPSHOT.jar
```

On Windows, replace `./mvnw` with `mvnw.cmd`.

Open **http://localhost:8080/admin/** and sign in with **admin / admin**. These credentials are sample defaults only. The sample uses an in-memory H2 database, so changes are lost when it restarts.

To integrate the starter into your own application, follow [installation](docs/installation.md), then [configuration](docs/configuration.md). The starter itself has no usable default password.

## Learn more

| For | Read |
| --- | --- |
| Application developers | [Installation](docs/installation.md), [configuration](docs/configuration.md), [entity customization](docs/entity-customization.md) |
| Administrators | [User guide](docs/user-guide.md), [deployment](docs/deployment.md), [troubleshooting](docs/troubleshooting.md) |
| API clients | [REST API reference](docs/api-reference.md), [authentication](docs/security.md) |
| Contributors | [Architecture](docs/architecture.md), [development](docs/development.md), [contributing](CONTRIBUTING.md) |
| Maintainers | [Release guide](docs/releasing.md) |

The [requirements document](PROJECT_REQUIREMENTS.md) records product plans; it is not a list of completed features. Published artifact availability is not verified here. No project license file is currently included; licensing terms must be supplied by the owner.

## Creator

Created and maintained by **[Durgesh Tiwari](https://github.com/durgeshcoder)** ([@durgeshcoder](https://github.com/durgeshcoder)).
