# Security and authentication

[Documentation index](README.md) · [Project overview](../README.md)

Applies to `1.0.0-SNAPSHOT` (Spring Boot 4.0.0).

1. The UI requests `/admin/api/auth/session` to check the session and obtain a CSRF token.
2. The login form submits the username and password to the server.
3. Spring Security checks the configured account using BCrypt password encoding and establishes a session.
4. The browser uses the session cookie for subsequent requests. Credentials are not stored in browser local storage.
5. Sign out invalidates the session. An expired session returns the UI to login.

The UI shell and static assets are public so that the login screen can load. Entity metadata, records, lookups, and actions require `ROLE_JPA_ADMIN`. CSRF protection applies to login, logout, and modifying API requests. The frontend refreshes its CSRF token after authentication.

### Existing Spring Security applications

The starter registers a dedicated security chain with order `-100` for `/admin/api/**` and a local authentication manager for the properties-based account. This is not an integration with your application's existing user database, SSO, or role assignments.

Ensure an earlier host security chain does not intercept the admin API. Host applications must explicitly protect their own routes: the starter's admin chain does not secure endpoints outside `/admin/api/**`. Its presence can cause Spring Boot's default catch-all chain to back off.

`@AdminPermission` is currently metadata only; its per-operation role arrays are **not enforced**. Field visibility, field `readOnly`, and custom-action scope are also not complete server-side authorization boundaries. Use the current admin console only for trusted administrators.

The demo H2 console configuration is unrelated to admin authentication and is not protected by the admin API chain.
