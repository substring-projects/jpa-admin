# Architecture

[Documentation index](README.md) · [Project overview](../README.md)

Applies to `1.0.0-SNAPSHOT` (Spring Boot 4.0.0).

```text
admin-for-springboot/
├── pom.xml                             # Parent and Maven reactor
├── jpa-admin-core/                     # Annotations, metadata scanner, CRUD service, API
├── jpa-admin-ui/                       # React application and static-resource JAR
├── jpa-admin-spring-boot-autoconfigure/ # Properties, MVC resources, session security
├── jpa-admin-spring-boot-starter/       # Consumer dependency aggregator
├── jpa-admin-sample-app/               # H2 demo and annotated entities
├── PROJECT_REQUIREMENTS.md            # Product vision and implementation roadmap
└── README.md
```

At startup, auto-configuration creates the metadata scanner and CRUD service. The scanner inspects managed JPA entities. The browser loads the packaged UI, authenticates, fetches metadata, and generates tables and forms that call the generic API.

Frontend assets are copied from `jpa-admin-ui/dist/` to `META-INF/resources/admin/` in the UI JAR during Maven's `process-resources` phase.
