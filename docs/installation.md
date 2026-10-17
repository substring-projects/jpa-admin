# Installation & JitPack Setup

[Documentation Index](README.md) · [Getting Started](getting-started.md) · [Configuration](configuration.md)

<div class="flex flex-wrap items-center gap-3 my-4 not-prose">
  <a href="https://jitpack.io/#substring-projects/jpa-admin" target="_blank" class="no-underline inline-block">
    <img src="https://img.shields.io/badge/JitPack-v1.0.1-2e7d32?style=flat-square&logo=jitpack" alt="JitPack Release" class="h-6 rounded">
  </a>
  <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-700">Spring Boot 3.x / 4.x</span>
  <span class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300 border border-blue-300 dark:border-blue-700">Java 17+</span>
</div>

---

## 1. Add JitPack Repository & Starter Dependency

<div class="tw-alert-danger not-prose my-6">
  <div class="flex items-start gap-3">
    <div class="text-2xl leading-none">⚠️</div>
    <div>
      <h4 class="font-bold text-red-600 dark:text-red-400 m-0 text-base">MANDATORY STEP: You MUST add the JitPack repository!</h4>
      <p class="text-sm mt-1.5 mb-2 text-slate-700 dark:text-slate-300 leading-normal">
        JitPack packages are <strong>NOT</strong> hosted on Maven Central. If you only copy the <code>&lt;dependency&gt;</code> without adding the <code>&lt;repositories&gt;</code> block pointing to <code>https://jitpack.io</code>, your build will <strong>fail immediately</strong> with:
      </p>
      <code class="text-xs text-red-600 dark:text-red-400 bg-red-100/60 dark:bg-red-950/70 px-2 py-1.5 rounded block border border-red-200 dark:border-red-900 font-mono">
        [ERROR] Could not find artifact com.github.substring-projects:jpa-admin-spring-boot-starter in central (https://repo.maven.apache.org/maven2)
      </code>
    </div>
  </div>
</div>

Use the tabs below to copy the complete configuration for your build tool:

=== "Maven (pom.xml)"

    Add **both** the `<repositories>` block and the starter `<dependency>` to your `pom.xml`:

    ```xml
    <!-- 1. MANDATORY: Add JitPack repository -->
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

    <!-- 2. Add JPA Admin Starter dependency -->
    <dependencies>
        <dependency>
            <groupId>com.github.substring-projects</groupId>
            <artifactId>jpa-admin-spring-boot-starter</artifactId>
            <version>v1.0.1</version>
        </dependency>
    </dependencies>
    ```

    > **Available Versions:**
    > 
    > • `v1.0.1` (Recommended stable release)  
    > • `main-SNAPSHOT` (Bleeding edge build of the main branch)

=== "Gradle (Groovy - build.gradle)"

    In your `build.gradle`, include the JitPack repository and the starter dependency:

    ```groovy
    // 1. MANDATORY: Add JitPack repository
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }

    // 2. Add JPA Admin Starter dependency
    dependencies {
        implementation 'com.github.substring-projects:jpa-admin-spring-boot-starter:v1.0.1'
    }
    ```

=== "Gradle (Kotlin DSL - build.gradle.kts)"

    In your `build.gradle.kts`:

    ```kotlin
    // 1. MANDATORY: Add JitPack repository
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    // 2. Add JPA Admin Starter dependency
    dependencies {
        implementation("com.github.substring-projects:jpa-admin-spring-boot-starter:v1.0.1")
    }
    ```

=== "Local Development (1.0.0-SNAPSHOT)"

    If you are building the library from source on your local machine:

    ```bash
    git clone https://github.com/substring-projects/jpa-admin.git
    cd jpa-admin
    ./mvnw clean install -DskipTests
    ```

    Then in your host application:

    ```xml
    <dependency>
        <groupId>io.github.substring-projects</groupId>
        <artifactId>jpa-admin-spring-boot-starter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    ```

---

## 2. Configure Admin Credentials

Configure authentication in your `application.yml` or `application.properties`:

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

---

## 3. Ensure Entity Scan Scope

The starter auto-discovers all JPA entities registered in your Spring `EntityManagerFactory`. No component scan of the starter is needed.

Ensure your entity package is in Spring's scan scope:

```java
@SpringBootApplication
@EntityScan(basePackages = "com.yourcompany.domain") // (if outside root package)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

## 4. Run & Access Admin UI

Start your Spring Boot application:

```bash
./mvnw spring-boot:run
```

Open your browser and navigate to:

👉 **`http://localhost:8080/admin/`**

Sign in with your credentials (`admin` / `admin123`). You will see all your JPA entities listed with full CRUD, search, and foreign key dropdown support!

---

## 5. JitPack Troubleshooting

<div class="space-y-4 my-4 not-prose">
  <div class="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900">
    <div class="font-bold text-slate-800 dark:text-slate-200 text-sm flex items-center gap-2">
      <span>❌</span> <span>Error: "Could not find artifact ... in central"</span>
    </div>
    <p class="text-xs text-slate-600 dark:text-slate-400 mt-1 mb-0 leading-relaxed">
      <strong>Fix:</strong> You forgot to add the <code>&lt;repositories&gt;</code> block with <code>https://jitpack.io</code>. Add the repository block shown in Step 1 to your <code>pom.xml</code> or <code>build.gradle</code>.
    </p>
  </div>

  <div class="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900">
    <div class="font-bold text-slate-800 dark:text-slate-200 text-sm flex items-center gap-2">
      <span>🔄</span> <span>Force Updating / Cache Refresh</span>
    </div>
    <p class="text-xs text-slate-600 dark:text-slate-400 mt-1 mb-0 leading-relaxed">
      If a new tag or commit was pushed and your build tool is using a cached version, force an update:<br/>
      • <strong>Maven:</strong> <code>mvn clean compile -U</code><br/>
      • <strong>Gradle:</strong> <code>./gradlew build --refresh-dependencies</code>
    </p>
  </div>

  <div class="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900">
    <div class="font-bold text-slate-800 dark:text-slate-200 text-sm flex items-center gap-2">
      <span>🌐</span> <span>Live Build Status</span>
    </div>
    <p class="text-xs text-slate-600 dark:text-slate-400 mt-1 mb-0 leading-relaxed">
      Visit the <a href="https://jitpack.io/#substring-projects/jpa-admin" target="_blank" class="text-indigo-600 dark:text-indigo-400 underline font-semibold">JitPack jpa-admin dashboard</a> to see live build logs for any tag or commit.
    </p>
  </div>
</div>
