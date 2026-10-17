# Installation & Quick Setup

[Documentation Index](README.md) · [Getting Started](getting-started.md) · [Configuration](configuration.md)

[![JitPack](https://jitpack.io/v/durgeshcoder/substring-projects.svg)](https://jitpack.io/#durgeshcoder/substring-projects)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x%20%2F%204.x-brightgreen.svg)
![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)

---

## 1. Add Dependency to Your Project

You can install `jpa-admin-spring-boot-starter` directly from **JitPack** (recommended for any external project) or install the snapshot locally via Maven.

=== "Maven (JitPack)"

    Add the JitPack repository to your `pom.xml`:

    ```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
    ```

    Then add the dependency:

    ```xml
    <dependency>
        <groupId>com.github.durgeshcoder.substring-projects</groupId>
        <artifactId>jpa-admin-spring-boot-starter</artifactId>
        <version>v1.0.1</version>
    </dependency>
    ```

    > **Note:** If you want to use the latest commit on `main`, you can specify `<version>main-SNAPSHOT</version>`. For stable builds, use release tag `v1.0.1`.

=== "Gradle (Groovy)"

    Add JitPack to your root `build.gradle`:

    ```groovy
    allprojects {
        repositories {
            mavenCentral()
            maven { url 'https://jitpack.io' }
        }
    }
    ```

    Add the starter to your `dependencies`:

    ```groovy
    dependencies {
        implementation 'com.github.durgeshcoder.substring-projects:jpa-admin-spring-boot-starter:v1.0.1'
    }
    ```

=== "Gradle (Kotlin DSL)"

    Add JitPack to `build.gradle.kts`:

    ```kotlin
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    dependencies {
        implementation("com.github.durgeshcoder.substring-projects:jpa-admin-spring-boot-starter:v1.0.1")
    }
    ```

=== "Local Maven (1.0.0-SNAPSHOT)"

    If you are developing or testing locally on your own machine, build the starter to your local `~/.m2` repository:

    ```bash
    git clone https://github.com/substring-projects/jpa-admin.git
    cd jpa-admin
    ./mvnw clean install -DskipTests
    ```

    Then reference the local snapshot in your host project:

    ```xml
    <dependency>
        <groupId>io.github.substring-projects</groupId>
        <artifactId>jpa-admin-spring-boot-starter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    ```

---

## 2. Configure Admin Credentials

By default, the starter requires an admin password to protect your data. Add the following to your application's `application.yml` or `application.properties`:

=== "application.yml"

    ```yaml
    spring:
      jpa:
        admin:
          enabled: true
          path: /admin
          security:
            enabled: true
            username: admin
            password: "${ADMIN_PASSWORD:admin123}"
    ```

=== "application.properties"

    ```properties
    spring.jpa.admin.enabled=true
    spring.jpa.admin.path=/admin
    spring.jpa.admin.security.enabled=true
    spring.jpa.admin.security.username=admin
    spring.jpa.admin.security.password=${ADMIN_PASSWORD:admin123}
    ```

> [!IMPORTANT]
> Always provide a secure password using environment variables (e.g. `ADMIN_PASSWORD`) in staging and production environments.

---

## 3. Verify Your Entity Scan

The starter auto-discovers all JPA entities registered in your Spring `EntityManagerFactory`. No component scan configuration is required.

Make sure your Spring Boot main application class has your entity packages in its scan scope:

```java
@SpringBootApplication
@EntityScan(basePackages = "com.yourcompany.domain") // (if outside standard package)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

## 4. Run & Access the Admin Dashboard

Start your Spring Boot application:

```bash
./mvnw spring-boot:run
```

Open your browser and navigate to:

👉 **`http://localhost:8080/admin/`**

Sign in with your configured username and password (`admin` / `admin123`). You will be greeted with the JPA Admin dashboard, listing all your JPA entities ready for CRUD operations!

---

## 5. JitPack Troubleshooting & Cache Refresh

If you push an update to your library and need to force your consumer project to download the newest build from JitPack:

1. **Force Maven Update**:
   ```bash
   mvn clean compile -U
   ```
2. **Force Gradle Refresh**:
   ```bash
   ./gradlew build --refresh-dependencies
   ```
3. **Check JitPack Build Status**:
   Visit `https://jitpack.io/#durgeshcoder/substring-projects` in your browser. Look up the tag or commit to inspect the JitPack compilation log in real-time.
