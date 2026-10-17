# Contributing

Thank you for contributing to Spring Boot JPA Admin. Start with the [documentation index](docs/README.md) and [development guide](docs/development.md).

## Local setup

Use JDK 17+, Node.js 20+, and npm. From the repository root:

```bash
npm ci --prefix jpa-admin-ui
npm test --prefix jpa-admin-ui
./mvnw clean verify
```

Use `mvnw.cmd` on Windows. Run the packaged sample and confirm that login and the affected feature work. For UI development, use the Vite workflow in the development guide.

## Propose a change

For a substantial change, open an issue describing the problem, expected behavior, and compatibility implications before implementing it. For a bug report, include the project version or commit, Java and Node versions, steps to reproduce, and relevant sanitized logs. Never include credentials, session cookies, or private records.

Keep a pull request focused on one problem. Describe the user-visible behavior, the affected modules, and how the change was verified. Add focused regression coverage for data binding, authentication, and other behavior that can cause data loss or access-control failures. For UI changes, check desktop and mobile layouts and include screenshots when useful.

Update the relevant documentation when changing configuration, annotation semantics, endpoints, or supported behavior. Keep planned features in the requirements document clearly separated from implemented functionality.

## Module responsibilities

| Module | Responsibility |
| --- | --- |
| `jpa-admin-core` | Annotations, metadata, CRUD, generic HTTP endpoints |
| `jpa-admin-spring-boot-autoconfigure` | Boot integration, configuration, authentication, resource serving |
| `jpa-admin-spring-boot-starter` | Consumer dependency aggregation |
| `jpa-admin-ui` | React application and static-resource packaging |
| `jpa-admin-sample-app` | Demonstration entities and integration coverage |

## Review checks

- Run relevant Java tests and frontend regression tests; run the frontend production build for UI changes.
- Keep credentials and machine-specific configuration out of commits.
- Review generated assets separately from source changes. JitPack currently packages committed `jpa-admin-ui/dist` assets, so release builds require those assets to match the source.
- State any checks that could not run and why.
- Verify relative documentation links and fenced examples before submitting documentation changes.

There is no project license file in this checkout. The owner must establish contribution and redistribution terms; this guide does not supply those terms.
