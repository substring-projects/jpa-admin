# Deployment

<p class="jpa-lead">The console is part of your application JAR. There's no separate server and no Node.js, so deploy your app as usual and check the items below.</p>

## Production checklist

- [x] **Password from a secret.** `spring.jpa.admin.security.password: ${ADMIN_PASSWORD}`, set by your platform's secret manager.
- [x] **HTTPS** in front of the application (load balancer, ingress or reverse proxy).
- [x] **Restrict access** to `{path}` with a VPN, SSO proxy or IP allow-list when the app faces the internet. The console has one account and no rate limiting.
- [x] **A real database** in place of the sample's H2, with schema changes managed by your migration tool (Flyway or Liquibase).
- [x] **Disable demo settings** such as the H2 console.
- [x] **Review editable entities.** Mark sensitive ones `readOnly = true` or `hidden = true` ([customizing](entity-customization.md#control-what-users-can-do)).
- [x] **Uploads:** use S3 or ImageKit, or a persistent volume for local storage. Configure bucket CORS and set `upload.signing-key` when running several instances.

## Several instances

| Concern | What to do |
| --- | --- |
| Sessions | Sessions live in each instance's memory. Use sticky sessions, or add [Spring Session](https://spring.io/projects/spring-session) (Redis or JDBC) to share them. |
| Upload tickets | Set the same `spring.jpa.admin.upload.signing-key` on every instance. |
| Uploaded files | Use S3, ImageKit or a shared volume. A local folder on one instance isn't visible to the others. |
| Saved views and recent records | Stored in each user's browser, so they're unaffected. |

## Containers

```dockerfile title="Dockerfile"
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/shop-app.jar app.jar
# Persist local uploads (skip when using S3 or ImageKit)
VOLUME /app/uploads
ENV SPRING_JPA_ADMIN_STORAGE_DEFAULT_DIRECTORY=/app/uploads
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Any property can be set through the environment with Spring Boot's relaxed binding:

| Property | Environment variable |
| --- | --- |
| `spring.jpa.admin.security.password` | `SPRING_JPA_ADMIN_SECURITY_PASSWORD` |
| `spring.jpa.admin.path` | `SPRING_JPA_ADMIN_PATH` |
| `spring.jpa.admin.primary-color` | `SPRING_JPA_ADMIN_PRIMARYCOLOR` |
| `spring.jpa.admin.upload.signing-key` | `SPRING_JPA_ADMIN_UPLOAD_SIGNINGKEY` |

## Behind a reverse proxy

The console works under its configured `path`. If the proxy adds its own prefix, for example `https://example.com/shop/admin` forwarding to `http://app:8080/admin`, set `server.servlet.context-path` or have the proxy preserve the path so the URLs the browser sees match the application's. Forward the `Host` and `X-Forwarded-*` headers and set `server.forward-headers-strategy=framework` so redirects and cookies use the public URL.

## Updating

The UI is versioned with the starter: upgrading the dependency upgrades the console. Read [What's new](whats-new.md) for breaking changes. Don't overwrite a running JAR in place; stop the process or deploy a new artifact.
