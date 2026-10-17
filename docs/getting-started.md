# Getting started

[Documentation index](README.md) · [Project overview](../README.md)

Applies to `1.0.0-SNAPSHOT` (Spring Boot 4.0.0).

## Requirements

| Tool     | Requirement                                                                                          |
| -------- | ---------------------------------------------------------------------------------------------------- |
| Java     | JDK 17 or newer; the Maven compiler targets Java 17.                                                 |
| Maven    | Maven Wrapper included (`./mvnw`), pinned to Maven 3.9.9. Use `mvnw.cmd` on Windows.                                     |
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
./mvnw clean package

# Start the sample application.
java -jar jpa-admin-sample-app/target/jpa-admin-sample-app-1.0.0-SNAPSHOT.jar
```

Open **http://localhost:8080/admin/**.

The sample credentials are:

| Field    | Value   |
| -------- | ------- |
| Username | `admin` |
| Password | `admin` |

These defaults are declared in [the sample application.yml](../jpa-admin-sample-app/src/main/resources/application.yml), not in frontend code. Replace them for any deployment beyond a local demo.

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
