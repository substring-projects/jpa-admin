# Deployment

<p class="jpa-lead">The console is part of your application JAR. There's no separate server and no Node.js, so deploy your app as usual and check the items below.</p>

## Production checklist

- [x] **Password from a secret.** `spring.jpa.admin.security.password: ${ADMIN_PASSWORD}`, set by your platform's secret manager.
- [x] **HTTPS** in front of the application (load balancer, ingress or reverse proxy).
- [x] **Restrict access** to `{path}` with a VPN, SSO proxy or IP allow-list when the app faces the internet. Configured mode has one account; database mode supports individual users/groups. Neither includes login rate limiting.
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


## Database identity rollout

For a team installation, add `jpa-admin-identity`, choose `mode: database`, and apply the [vendor-specific identity migration](database-setup.md) before starting with `initialize-schema: false`. Keep dependency versions aligned. Existing accounts survive restarts in a persistent database; bootstrap settings do not overwrite passwords.

Back up identity tables alongside business records. Restore users, groups, memberships and grants consistently, and plan session invalidation when restoring older account versions. A database restore does not restore or revoke session cookies by itself. There is no built-in identity backup/restore or password-recovery command.

All instances must use the intended shared identity database for consistent accounts and permissions. Identity tables do **not** store HTTP sessions: configure session persistence separately. Account/group mutations are serialized by a database row lock; do not remove the coordination table or route identity traffic to a read-only replica. Large-scale account administration is not paginated in this version.

For upgrades, preserve stable permission namespaces and test reader/editor accounts in addition to a superuser. Changing from database to configured mode changes the access model; it is not an equivalent fallback. Verify datasource, security-chain and migration behavior in staging with your actual database version.
