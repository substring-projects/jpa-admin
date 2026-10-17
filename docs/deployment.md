# Deployment

[Documentation index](README.md) · [Project overview](../README.md)

Applies to `1.0.0-SNAPSHOT` (Spring Boot 4.0.0).

Build the artifacts, then run the host application's JAR with a configured database and admin credentials. Node.js is not required on the runtime host because the UI is served as static resources.

For a deployment beyond the demo:

- Supply a unique admin password through environment or secret configuration and serve the application over HTTPS.
- Replace the sample H2 datasource with your persistent database configuration; manage schema changes through your application's migration process.
- Disable the sample H2 console and define access controls for all non-admin routes.
- Keep the `/admin` prefix and a root application context until custom-path support is completed across the frontend, API, and security configuration.
- Account for session affinity or shared session storage if running multiple instances; distributed sessions are not configured by this starter.
- Review the documented authorization and payload-binding limitations before exposing business entities.

Do not rebuild an executable JAR in place while it is running. Stop the process first or run a separate copy of the artifact, then restart with the new build.
