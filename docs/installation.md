# Installation

<p class="jpa-lead">Add the starter to an existing Spring Boot application. The console appears at <code>/admin/</code> on the next start.</p>

## Requirements

| | |
| --- | --- |
| **Java** | 17 or newer |
| **Spring Boot** | 4.0 (the version this project is built and tested with). Spring Boot 3 is not verified. |
| **Application type** | Servlet web application (Spring MVC). WebFlux is not supported. |
| **Persistence** | Spring Data JPA / Hibernate with your own datasource and entities |
| **Runtime** | No Node.js needed; the starter includes the UI dependency. |

The starter brings Spring Web, Spring Data JPA, Validation and Spring Security.

## 1. Add the dependency

Use the `com.substringtechnologies` coordinates for the `0.1.0` Maven Central release. Maven uses Central by default; Gradle needs `mavenCentral()`.

Version `0.1.0` is published on [Maven Central](https://central.sonatype.com/artifact/com.substringtechnologies/jpa-admin-spring-boot-starter/0.1.0). The parent POM and all four library modules are publicly available. No JitPack repository is needed.

=== "Maven"

    ```xml title="pom.xml"
    <dependency>
        <groupId>com.substringtechnologies</groupId>
        <artifactId>jpa-admin-spring-boot-starter</artifactId>
        <version>0.1.0</version>
    </dependency>
    ```

=== "Gradle (Kotlin)"

    ```kotlin title="build.gradle.kts"
    repositories { mavenCentral() }
    dependencies {
        implementation("com.substringtechnologies:jpa-admin-spring-boot-starter:0.1.0")
    }
    ```

=== "Gradle (Groovy)"

    ```groovy title="build.gradle"
    repositories { mavenCentral() }
    dependencies {
        implementation 'com.substringtechnologies:jpa-admin-spring-boot-starter:0.1.0'
    }
    ```

=== "Built from source"

    Requires JDK 17+, Node.js 20+ and npm. The current development checkout uses `0.2.0-SNAPSHOT`:

    ```bash
    git clone https://github.com/substring-projects/jpa-admin.git
    cd jpa-admin
    npm ci --prefix jpa-admin-ui
    ./mvnw clean install
    ```

    Then use the same Maven dependency above with `<version>0.2.0-SNAPSHOT</version>`. This version is installed only on your machine. For Gradle local testing, add `mavenLocal()` before `mavenCentral()` and use version `0.2.0-SNAPSHOT`.

The starter downloads core, auto-configuration and UI modules transitively. The UI assets live in the `jpa-admin-ui` JAR; consumers do not build React or install Node.js.

Older JitPack builds used `com.github.substring-projects.jpa-admin`. When migrating, replace the old dependency with the Central coordinates above and remove the JitPack repository if no other dependency needs it. The version sequences differ: `0.1.0` is the first Central release, not a downgrade of the older JitPack tags.

??? tip "Using Amazon S3 for uploads?"
    Also add the AWS SDK. It's optional, so applications that don't use S3 don't pay for it:

    ```xml
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
        <version>2.46.7</version>
    </dependency>
    ```

## 2. Set the admin login

The starter refuses to start without a password, so there is no insecure default.

=== "application.yml"

    ```yaml
    spring:
      jpa:
        admin:
          security:
            username: admin
            password: ${ADMIN_PASSWORD}   # supply through the environment
    ```

=== "application.properties"

    ```properties
    spring.jpa.admin.security.username=admin
    spring.jpa.admin.security.password=${ADMIN_PASSWORD}
    ```

```bash
export ADMIN_PASSWORD='choose-a-strong-password'
```

These credentials are for the admin console only. They are separate from your datasource credentials and from any users in your own application. See [Configuration](configuration.md) for every option.

## 3. Make sure your entities are scanned

The console shows every entity in your `EntityManagerFactory`, so no extra scanning is needed. If your entities live outside the package of your `@SpringBootApplication` class, register them as usual:

```java
@SpringBootApplication
@EntityScan("com.example.shop.domain")
public class ShopApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }
}
```

## 4. Open the console

```bash
./mvnw spring-boot:run
```

Open **`http://localhost:8080/admin/`** and sign in. Every entity is listed in the sidebar, grouped and ready to use.

!!! tip "Next steps"
    - [Take the tour](user-guide.md) to see everything the console can do.
    - [Customize entities](entity-customization.md) with labels, groups, badges and actions.
    - [Add file uploads](file-uploads.md) for images and documents.

## Troubleshooting dependency resolution

| Problem | Fix |
| --- | --- |
| `Could not find artifact … in central` | Check the coordinates and publication status. Once available, run `./mvnw -U compile` to retry a cached failure. |
| Gradle cannot resolve the starter | Add `mavenCentral()`; use `--refresh-dependencies` after publication. |
| A local source build works but another machine fails | Local `install` is not publication. Verify Central availability using a fresh local repository (see [Release guide](releasing.md)). |
