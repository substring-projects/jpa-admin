# Install the starter

[Documentation index](README.md) · [Getting started](getting-started.md)

## Build and install locally

The repository version is `1.0.0-SNAPSHOT`. From the repository root, run:

```bash
npm ci --prefix jpa-admin-ui
npm test --prefix jpa-admin-ui
./mvnw clean install
```

This installs the modules into your local Maven repository and builds the sample application. On Windows, use `mvnw.cmd`. The wrapper's first run downloads Maven and requires network access.

Add the dependency to a Spring Boot 4.0.0 application:

```xml
<dependency>
    <groupId>io.github.substring-projects</groupId>
    <artifactId>jpa-admin-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

For Gradle Kotlin DSL, using the same locally installed artifacts:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.github.substring-projects:jpa-admin-spring-boot-starter:1.0.0-SNAPSHOT")
}
```

Provide a datasource, its JDBC driver, and JPA entities in the application's entity-scan scope. The starter brings Web, Data JPA, Validation, and Security dependencies transitively. No manual component scan of the starter packages is required.

Add credentials to the host application's configuration:

```yaml
spring:
  jpa:
    admin:
      security:
        username: "${ADMIN_USERNAME:admin}"
        password: "${ADMIN_PASSWORD}"
```

Supply `ADMIN_PASSWORD` through your environment or deployment secret configuration. Start your application and open `/admin/`. See [configuration](configuration.md) and [security](security.md) before integrating with an existing application.

## Published packages

The repository includes [JitPack build configuration](../jitpack.yml), which builds the starter and required modules using committed UI assets and skips tests and the npm build. A successful JitPack build is not evidence that tests passed.

This documentation does not certify a published release or Maven Central availability. For JitPack distribution, obtain the exact module coordinates from a successful build for the intended tag or commit and verify resolution from a clean consumer project. Do not assume local Maven coordinates are the same as JitPack coordinates. Prefer an immutable release or commit over a moving branch.

See the [release guide](releasing.md) for publication checks.
