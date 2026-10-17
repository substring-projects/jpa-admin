# Installation & Quick Setup

[Documentation Index](README.md) · [Getting Started](getting-started.md) · [Configuration](configuration.md)

<div class="flex flex-wrap items-center gap-2 my-4">
  <a href="https://jitpack.io/#durgeshcoder/substring-projects"><img src="https://jitpack.io/v/durgeshcoder/substring-projects.svg" alt="JitPack Release" class="h-6"></a>
  <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-700">Spring Boot 3.x / 4.x</span>
  <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300 border border-blue-300 dark:border-blue-700">Java 17+</span>
</div>

---

## 1. Add Dependency & JitPack Repository

<div class="tw-alert-danger">
  <div class="flex items-start gap-3">
    <div class="text-xl">⚠️</div>
    <div>
      <h4 class="font-bold text-red-600 dark:text-red-400 m-0 p-0 text-base">CRITICAL STEP: You MUST add the JitPack repository!</h4>
      <p class="text-sm mt-1 mb-0 text-slate-700 dark:text-slate-300">
        JitPack artifacts are <strong>NOT</strong> hosted on Maven Central. If you only copy the <code>&lt;dependency&gt;</code> without defining the <code>&lt;repositories&gt;</code> block pointing to <code>https://jitpack.io</code>, your build will <strong>immediately fail</strong> with:
        <br/><code class="text-xs text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-950/50 p-1 rounded mt-1 inline-block">Could not find artifact com.github.durgeshcoder.substring-projects:jpa-admin-spring-boot-starter in central (https://repo.maven.apache.org/maven2)</code>
      </p>
    </div>
  </div>
</div>

=== "Maven (JitPack) - Complete pom.xml"

    In your consumer application's `pom.xml`, include **both** the `<repositories>` block and the `<dependency>`:

    ```xml
    <!-- 1. ADD JITPACK REPOSITORY (MANDATORY - Otherwise build fails!) -->
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

    <!-- 2. ADD JPA ADMIN STARTER DEPENDENCY -->
    <dependencies>
        <dependency>
            <groupId>com.github.durgeshcoder.substring-projects</groupId>
            <artifactId>jpa-admin-spring-boot-starter</artifactId>
            <version>v1.0.1</version>
        </dependency>
    </dependencies>
    ```

    > **Tip:** You can use version `v1.0.1` (recommended stable tag) or `main-SNAPSHOT` (latest build from the main branch).

=== "Gradle (Groovy) - build.gradle"

    In your root `build.gradle`, add JitPack to your `repositories` block and then declare the starter:

    ```groovy
    // 1. ADD JITPACK REPOSITORY (MANDATORY)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }

    // 2. ADD DEPENDENCY
    dependencies {
        implementation 'com.github.durgeshcoder.substring-projects:jpa-admin-spring-boot-starter:v1.0.1'
    }
    ```

=== "Gradle (Kotlin DSL) - build.gradle.kts"

    In your `build.gradle.kts`:

    ```kotlin
    // 1. ADD JITPACK REPOSITORY (MANDATORY)
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    // 2. ADD DEPENDENCY
    dependencies {
        implementation("com.github.durgeshcoder.substring-projects:jpa-admin-spring-boot-starter:v1.0.1")
    }
    ```

=== "Local Maven (1.0.0-SNAPSHOT)"

    If you are building and testing locally on the same development machine:

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

By default, the starter secures all administration endpoints. Configure the admin password in your `application.yml` or `application.properties`:

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

> [!TIP]
> Use an environment variable like `ADMIN_PASSWORD` so credentials aren't committed to version control.

---

## 3. Auto-Discovery of Entities

Spring Boot JPA Admin automatically detects all entities mapped by your Spring Data JPA configuration (`EntityManagerFactory`).

Make sure your entity package is covered by your application's entity scan:

```java
package com.example.myproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "com.example.myproject.domain")
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

---

## 4. Run & Access Your Admin Dashboard

Start your application:

```bash
./mvnw spring-boot:run
```

Open your browser and navigate to:

👉 **`http://localhost:8080/admin/`**

Sign in with your credentials (`admin` / `admin123`). The dashboard will instantly display all registered JPA entities with full CRUD support, search, sorting, and foreign-key relation pickers!

---

## 5. JitPack Troubleshooting Guide

<div class="space-y-4 my-4">
  <div class="p-4 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900">
    <h4 class="font-bold text-slate-800 dark:text-slate-200 mt-0 text-sm">Issue: "Could not find artifact ... in central"</h4>
    <p class="text-xs text-slate-600 dark:text-slate-400 mb-0">
      <strong>Solution:</strong> You did not add <code>&lt;repositories&gt;&lt;repository&gt;&lt;id&gt;jitpack.io&lt;/id&gt;&lt;url&gt;https://jitpack.io&lt;/url&gt;&lt;/repository&gt;&lt;/repositories&gt;</code> to your <code>pom.xml</code>. Maven will only check Maven Central unless JitPack is explicitly declared.
    </p>
  </div>

  <div class="p-4 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900">
    <h4 class="font-bold text-slate-800 dark:text-slate-200 mt-0 text-sm">Issue: Cache holding an older build</h4>
    <p class="text-xs text-slate-600 dark:text-slate-400 mb-0">
      <strong>Solution:</strong> Force your build tool to re-check the remote repository:<br/>
      • Maven: <code>mvn clean compile -U</code><br/>
      • Gradle: <code>./gradlew build --refresh-dependencies</code>
    </p>
  </div>

  <div class="p-4 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900">
    <h4 class="font-bold text-slate-800 dark:text-slate-200 mt-0 text-sm">Issue: Checking real-time build status</h4>
    <p class="text-xs text-slate-600 dark:text-slate-400 mb-0">
      Visit <a href="https://jitpack.io/#durgeshcoder/substring-projects" target="_blank" class="text-indigo-600 dark:text-indigo-400 underline">JitPack Substring-Projects Dashboard</a> to inspect the build log for any specific tag or commit.
    </p>
  </div>
</div>
