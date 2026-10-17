# Spring Boot JPA Admin

<div class="hero-card">
  <div class="hero-title">Spring Boot JPA Admin</div>
  <p style="font-size: 1.15rem; color: var(--md-default-fg-color--light); margin-bottom: 1.25rem;">
    An enterprise-grade, auto-generated administration UI and RESTful management engine for Spring Boot JPA applications.
  </p>
  <div>
    <span class="badge-chip">🚀 Spring Boot 3.x / 4.x</span>
    <span class="badge-chip">⚡ Java 17+</span>
    <span class="badge-chip">⚛️ React 19 & Tailwind</span>
    <span class="badge-chip">📦 Zero-Config Autowiring</span>
    <span class="badge-chip">🔒 Built-in Security & CSRF</span>
  </div>
</div>

---

## Key Capabilities

<div class="feature-grid">
  <div class="feature-box">
    <h4>⚡ Instant Administration</h4>
    <p>Zero boilerplates. Automatically scans your JPA metamodel and renders production-ready CRUD tables, interactive forms, and relation pickers.</p>
  </div>
  <div class="feature-box">
    <h4>🔗 Intelligent Relations</h4>
    <p>Seamlessly handles <code>@ManyToOne</code> and <code>@OneToOne</code> foreign keys with live searchable selectboxes, respecting entity <code>toString()</code> methods.</p>
  </div>
  <div class="feature-box">
    <h4>🛡️ Enterprise Security</h4>
    <p>Cookie-based sessions, CSRF token validation, role-based authorization, and automatic masking of sensitive attributes like passwords.</p>
  </div>
  <div class="feature-box">
    <h4>🎨 Rich Customization</h4>
    <p>Intuitive annotations like <code>@AdminResource</code>, <code>@AdminField</code>, <code>@AdminBadge</code>, and <code>@AdminAction</code> for complete UI control.</p>
  </div>
  <div class="feature-box">
    <h4>⚡ Standalone Bundled UI</h4>
    <p>The modern React frontend is pre-compiled and served directly from the Spring Boot starter JAR with zero Node/npm runtime dependency.</p>
  </div>
  <div class="feature-box">
    <h4>📊 Dynamic Filtering & Sorting</h4>
    <p>Real-time keyword search, enum filters, pagination controls, and custom bulk actions out-of-the-box.</p>
  </div>
</div>

---

## Quick Navigation

| What are you looking for? | Guide Link |
| :--- | :--- |
| **Install via JitPack or Maven** | [Installation Guide](installation.md) |
| **Run the Demo Application** | [Getting Started](getting-started.md) |
| **Configure Security & Paths** | [Configuration Reference](configuration.md) |
| **Customize Entities & Badges** | [Entity Customization](entity-customization.md) |
| **REST API Documentation** | [API Reference](api-reference.md) |
| **Authentication & Permissions** | [Security Architecture](security.md) |
| **Production Deployment Checklist** | [Deployment Guide](deployment.md) |
| **Troubleshooting & Fixes** | [Troubleshooting](troubleshooting.md) |
| **Preview & Publish Documentation** | [Documentation Website](publishing-docs.md) |
| **Contributing Code & Pull Requests** | [Contributing](../CONTRIBUTING.md) |

---

## 30-Second Example

Add the starter dependency via JitPack:

```xml
<dependency>
    <groupId>com.github.durgeshcoder.substring-projects</groupId>
    <artifactId>jpa-admin-spring-boot-starter</artifactId>
    <version>v1.0.1</version>
</dependency>
```

Annotate your standard JPA entity:

```java
@Entity
@AdminResource(displayName = "Product", group = "Catalog", icon = "package")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @AdminField(label = "Product Title", searchable = true, sortable = true)
    private String name;

    @ManyToOne
    @AdminField(label = "Category")
    private Category category;

    @AdminBadge(mapping = {
        @BadgeMap(value = "ACTIVE", color = BadgeColor.GREEN, label = "Active"),
        @BadgeMap(value = "ARCHIVED", color = BadgeColor.GRAY, label = "Archived")
    })
    private ProductStatus status;

    @Override
    public String toString() {
        return name;
    }
}
```

Start your Spring Boot app and open **`http://localhost:8080/admin/`** to view your dashboard!
