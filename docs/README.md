# Spring Boot JPA Admin

<div class="hero-card not-prose">
  <div class="hero-title">Spring Boot JPA Admin</div>
  <p class="text-lg text-slate-600 dark:text-slate-300 mb-6 max-w-2xl leading-relaxed">
    Instant, production-ready administration UI and dynamic REST management engine for Spring Boot JPA applications. Zero boilerplate, zero frontend setup.
  </p>
  
  <div class="flex flex-wrap items-center gap-3 mb-6">
    <a href="https://jitpack.io/#substring-projects/jpa-admin" target="_blank" class="no-underline inline-block">
      <img src="https://img.shields.io/badge/JitPack-v1.0.1-2e7d32?style=flat-square&logo=jitpack" alt="JitPack Release" class="h-6 rounded">
    </a>
    <span class="inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-700">
      Spring Boot 3.x / 4.x
    </span>
    <span class="inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300 border border-blue-300 dark:border-blue-700">
      Java 17+
    </span>
    <span class="inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold bg-indigo-100 text-indigo-800 dark:bg-indigo-950 dark:text-indigo-300 border border-indigo-300 dark:border-indigo-700">
      React 19 & Tailwind
    </span>
  </div>

  <div class="flex flex-wrap items-center gap-3">
    <a href="installation/" class="inline-flex items-center justify-center px-5 py-2.5 rounded-lg text-sm font-semibold bg-indigo-600 hover:bg-indigo-700 text-white shadow-sm transition-all duration-150 no-underline">
      🚀 Get Started
    </a>
    <a href="https://github.com/substring-projects/jpa-admin" target="_blank" class="inline-flex items-center justify-center px-5 py-2.5 rounded-lg text-sm font-semibold border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-800 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800 transition-all duration-150 no-underline">
      📦 GitHub Repository
    </a>
  </div>
</div>

---

## Console Preview

=== "Dashboard"

    ![JPA Admin Dashboard](assets/images/dashboard.png)
    *Auto-discovers JPA entities with KPI metrics and registered domain entity cards.*

=== "Entity Table & Filters"

    ![Entity Table & Filters](assets/images/entity-table.png)
    *Full-text search, column sorting, pagination controls, and scalar filters.*

=== "Foreign Key Relations"

    ![Foreign Key Selectbox](assets/images/relation-select.png)
    *Searchable relation selectbox for `@ManyToOne` / `@OneToOne` using descriptive `toString()` labels.*

=== "Batch Actions & Selection"

    ![Batch Selection](assets/images/batch-operations.png)
    *Multi-record row selection for bulk operations with safe deletion confirmation modals.*

=== "Authentication & Security"

    ![Login View](assets/images/login.png)
    *Protected administrator login with CSRF token rotation and secure session management.*

---

## Why Spring Boot JPA Admin?

<div class="feature-grid not-prose">
  <div class="feature-box">
    <h4 class="text-slate-900 dark:text-white">⚛️ Smooth Single Page Application (SPA)</h4>
    <p class="text-sm text-slate-600 dark:text-slate-400 m-0">Built with <strong>React 19 & Vite</strong>. Seamless client-side navigation without full-page reloads, snappy view transitions, skeleton loaders, and instant UI state updates.</p>
  </div>
  <div class="feature-box">
    <h4 class="text-slate-900 dark:text-white">🎨 Modern Tailwind CSS & Radix UI</h4>
    <p class="text-sm text-slate-600 dark:text-slate-400 m-0">Clean enterprise design styled with <strong>Tailwind CSS</strong> and accessible <strong>Radix UI</strong> primitives. Features responsive sidebars, mobile drawer navigation, modals, and toasts.</p>
  </div>
  <div class="feature-box">
    <h4 class="text-slate-900 dark:text-white">🌓 Seamless Light & Dark Theme</h4>
    <p class="text-sm text-slate-600 dark:text-slate-400 m-0">Effortless one-click switching between classic Slate Light and deep Onyx Dark modes. Preferences persist across sessions with zero theme flicker.</p>
  </div>
  <div class="feature-box">
    <h4 class="text-slate-900 dark:text-white">⚡ Instant Administration</h4>
    <p class="text-sm text-slate-600 dark:text-slate-400 m-0">Auto-discovers your entities via the JPA Metamodel on startup and generates fully functional CRUD tables, search bars, pagination, and edit forms with zero code.</p>
  </div>
  <div class="feature-box">
    <h4 class="text-slate-900 dark:text-white">🔗 Smart Foreign Key Relations</h4>
    <p class="text-sm text-slate-600 dark:text-slate-400 m-0">Handles <code>@ManyToOne</code> and <code>@OneToOne</code> relations with searchable async dropdowns. Automatically calls your entity's <code>toString()</code> for readable labels.</p>
  </div>
  <div class="feature-box">
    <h4 class="text-slate-900 dark:text-white">🛡️ Built-in Security & CSRF</h4>
    <p class="text-sm text-slate-600 dark:text-slate-400 m-0">Cookie-backed session authentication, automatic CSRF token validation, role-based entity permissions, and automatic masking of sensitive password fields.</p>
  </div>
  <div class="feature-box">
    <h4 class="text-slate-900 dark:text-white">📦 Zero Runtime Node.js Dependency</h4>
    <p class="text-sm text-slate-600 dark:text-slate-400 m-0">The modern React 19 UI is pre-compiled and served directly from the Spring Boot starter JAR as static classpath assets. No Node.js or npm required at runtime.</p>
  </div>
  <div class="feature-box">
    <h4 class="text-slate-900 dark:text-white">🎨 Declarative Customization</h4>
    <p class="text-sm text-slate-600 dark:text-slate-400 m-0">Fine-tune behavior with lightweight annotations: <code>@AdminResource</code>, <code>@AdminField</code>, <code>@AdminBadge</code>, <code>@AdminAction</code>, and <code>@AdminWidget</code>.</p>
  </div>
  <div class="feature-box">
    <h4 class="text-slate-900 dark:text-white">🔍 Dynamic Search, Sort & Filters</h4>
    <p class="text-sm text-slate-600 dark:text-slate-400 m-0">Debounce-accelerated full-text search, multi-column sorting, scalar attribute filters, and customizable page sizes.</p>
  </div>
  <div class="feature-box">
    <h4 class="text-slate-900 dark:text-white">🗂️ Batch Operations & Safe Deletions</h4>
    <p class="text-sm text-slate-600 dark:text-slate-400 m-0">Multi-record row selection for bulk operations with safe deletion confirmation modals to prevent accidental data loss.</p>
  </div>
  <div class="feature-box">
    <h4 class="text-slate-900 dark:text-white">🔌 Headless REST Management API</h4>
    <p class="text-sm text-slate-600 dark:text-slate-400 m-0">Complete REST management API exposed under <code>/admin/api/**</code> for metadata inspection, schema discovery, and programmatic CRUD integration.</p>
  </div>
  <div class="feature-box">
    <h4 class="text-slate-900 dark:text-white">🧩 Type-Aware Form Controls</h4>
    <p class="text-sm text-slate-600 dark:text-slate-400 m-0">Automatic form generation with datetime formatters, toggle switches for booleans, enum selects, textareas, and password reveal controls.</p>
  </div>
</div>

---

## 30-Second Quickstart

### 1. Add Repository & Starter Dependency

=== "Maven (pom.xml)"

    ```xml
    <!-- Mandatory: Add JitPack repository -->
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

    <!-- Add the starter -->
    <dependencies>
        <dependency>
            <groupId>com.github.substring-projects.jpa-admin</groupId>
            <artifactId>jpa-admin-spring-boot-starter</artifactId>
            <version>v1.0.1</version>
        </dependency>
    </dependencies>
    ```

=== "Gradle (build.gradle)"

    ```groovy
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }

    dependencies {
        implementation 'com.github.substring-projects.jpa-admin:jpa-admin-spring-boot-starter:v1.0.1'
    }
    ```

### 2. Configure Credentials

In `application.yml`:

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

### 3. Open Admin Dashboard

Run your Spring Boot application and open:

👉 **`http://localhost:8080/admin/`**

---

## Documentation Directory

Browse the complete documentation using the topics in the left sidebar:

| Category | Topics |
| :--- | :--- |
| **Getting Started** | [Installation & JitPack Setup](installation.md) · [Running the Demo App](getting-started.md) · [Configuration Options](configuration.md) |
| **Guides & Features** | [User Guide](user-guide.md) · [Entity Customization](entity-customization.md) · [Security & Auth](security.md) · [REST API Reference](api-reference.md) |
| **Operations** | [Deployment Guide](deployment.md) · [Troubleshooting](troubleshooting.md) · [Limitations & Roadmap](limitations.md) |
| **Developers** | [Architecture](architecture.md) · [Local Development](development.md) · [Release Guide](releasing.md) · [Contributing](../CONTRIBUTING.md) |
