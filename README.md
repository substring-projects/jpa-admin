# Spring Boot JPA Admin

A Spring Boot starter that discovers JPA entities and generates an administration console for browsing, creating, updating, and deleting records. The React UI is embedded in a JAR and served by your Spring application at `/admin/`.

**Version:** `1.0.0-SNAPSHOT` · **Java:** 17+ · **Spring Boot:** 4.0.0

[Documentation](docs/README.md) · [Installation](docs/installation.md) · [API reference](docs/api-reference.md) · [Contributing](CONTRIBUTING.md)

## Capabilities

- Metadata-driven tables and forms, pagination, sorting, text search, and scalar filters.
- Searchable single-value relationships and read-only inline collections.
- Entity and field customization through annotations, status badges, and custom actions.
- Session authentication, CSRF protection, and a configured administrator account.
- Responsive navigation, light/dark themes, and packaged frontend assets.

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
