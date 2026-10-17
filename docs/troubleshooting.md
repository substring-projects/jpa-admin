# Troubleshooting

[Documentation index](README.md) · [Project overview](../README.md)

Applies to `1.0.0-SNAPSHOT` (Spring Boot 4.0.0).

| Problem                                      | What to check                                                                                                                                               |
| -------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Startup asks for admin credentials           | Set `spring.jpa.admin.security.password` to a non-blank value. Set `ADMIN_PASSWORD` when using the documented placeholder.                                  |
| Default login does not work                  | `admin/admin` is only the sample fallback. Environment overrides or your host application's properties take precedence. Restart after changing credentials. |
| UI says it cannot connect                    | Confirm the backend is running. In development, check Vite's port-8080 API proxy. Rebuild/restart an older backend that lacks the session endpoint.         |
| API returns `401`                            | Sign in and retain the session cookie. Refreshing with an expired session requires another login.                                                           |
| API returns `403`                            | Fetch a fresh session/CSRF token, keep its cookie, and send the token header on modifying requests. Check host filter-chain ordering and authorities.       |
| `npm` or `tsc` is missing during Maven build | Install Node/npm and run `npm ci --prefix jpa-admin-ui` before Maven.                                                                                       |
| An entity is missing                         | Check entity scanning and datasource configuration, and ensure `@AdminResource(hidden = true)` is not set. Restart after entity changes.                    |
| No search results or unexpected filters      | Search operates on eligible string fields; filters are exact-value scalar comparisons. Relation and range filters are incomplete.                           |
| Custom URL prefix fails                      | Keep `spring.jpa.admin.path=/admin`; API and asset paths are not fully configurable yet.                                                                    |
| UI changes do not appear in the JAR          | Run `./mvnw package` and restart using the newly packaged sample/host artifact. Use Vite for live frontend work.                                               |
| Port 8080 is busy                            | Stop the process you own or start the sample with `--server.port=8081`; update the Vite proxy if needed.                                                    |
| Sample records disappear after restart       | Expected for the demo's in-memory H2 database. Configure a persistent datasource to retain data.                                                            |
| An action method fails                       | Use an instance no-argument method or a static method taking one `List`; additional injected parameters are unsupported.                                    |
| An inline row cannot be edited               | Inline collections are currently read-only. Use the child entity's own table and form.                                                                      |
