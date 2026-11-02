# 0.2.0 release checklist

Status: **published on Maven Central on 2026-11-02**. This page records the release scope, migration requirements and verification evidence. Building/installing locally does not publish to Maven Central.

## Release contents

Publish these artifacts together under `com.substringtechnologies`, all at `0.2.0`:

| Artifact | Role |
| --- | --- |
| `jpa-admin-parent` | Parent POM and dependency management |
| `jpa-admin-core` | Annotations, metadata, CRUD, storage and authorization contracts |
| `jpa-admin-ui` | Compiled console assets |
| `jpa-admin-spring-boot-autoconfigure` | Configuration, isolated admin security and identity APIs |
| `jpa-admin-spring-boot-starter` | Consumer dependency entry point |
| `jpa-admin-identity` | Optional H2/PostgreSQL/MySQL identity adapter |

The sample application is built for verification but excluded from Central deployment. React package metadata is aligned to 0.2.0; it is private and is not an npm publication.

## Highlights

- Dedicated admin accounts, groups, superusers, stable permission namespaces and server-enforced entity/action permissions.
- H2/PostgreSQL/MySQL identity schemas, custom identity adapters and a Users and Groups management screen.
- Admin and host security isolation, context-path-aware UI, and login-attempt throttling.
- Widget bounds, sliders, language-aware code editors, thumbnail sizes, stacked inlines, computed formatting, sensitive-field options, action icons, configurable lookups and Java customization hooks.
- API limits, controlled error responses, and fixes for masked inline values and sensitive-field sorting.
- Expanded setup, operations, permissions and extension documentation; CI builds for Java 17/21/25 and real database contracts.

## Migration from 0.1.0

| Change | Required action |
| --- | --- |
| `@AdminPermission` removed | Remove imports/usages. Its role arrays were never enforced. Use database groups and stable `permissionResource` keys. |
| `@AdminLookup(quickCreate)` removed | Remove the annotation member; related-record quick creation remains unavailable. |
| Entity metadata `roles` removed | Update clients that deserialize or depend on the old metadata property. |
| `@AdminResource(pageSize)` defaults to `0` | `0` now inherits global `spring.jpa.admin.page-size`. Explicit entity values still override it. |
| Unexpected failures now return generic `500` | Update API clients/tests that previously expected `400` or exception details. |
| Action business rejections now return `409` | Handle action `IllegalArgumentException`/`IllegalStateException` as a conflict. |
| Page/batch limit 500, lookup limit 100 | Update large API consumers to respect request limits. |
| Login throttling | Handle `429` and `Retry-After`; configure `security.login.*` for deployment needs. |
| Newly honored annotation options | Review existing annotation values that previously had no effect, especially widget bounds, sensitive `mask=false`, computed formats and lookup search fields. |
| Upload ticket owner/operation claims | Retry pre-upgrade uploads; old tickets are not compatible. |
| UI bundle no longer tracked in Git | Build from locked npm dependencies; a source checkout needs Node/npm to produce the UI JAR. Consumers still do not need Node at runtime. |
| Database identity is optional | Keep configured mode unless intentionally adopting accounts/groups. For database mode, add identity or a custom store, apply the right vendor migration, and bootstrap once. |

Read [What's new](whats-new.md), [database setup](database-setup.md), [host security integration](security.md#coexisting-with-host-spring-security) and [limitations](limitations.md). Test at least a reader/editor account, not only a superuser. Back up persistent identity/business data before rollout.

## Local verification

Local preparation used JDK 25.0.3, Node 24.4.0 and installed Chrome. The independent consumer used JDK 21.0.1. Artifacts compile to Java 17 bytecode; Java 17 runtime execution remains a CI gate, not a locally executed check.

| Check | Result |
| --- | --- |
| Clean release profile: `mvn -Prelease -Dgpg.skip=true clean install` | Passed; 68 backend tests, no failures/skips |
| UI unit tests | 12 passed |
| Complete Chrome suite, configured mode | 22 passed; 4 database-only tests intentionally skipped |
| Complete Chrome suite, database mode | 26 passed |
| Real PostgreSQL 18 identity contract | 6 passed |
| Real MySQL 8.4 identity contract | 6 passed |
| Separate Spring Boot 4 consumer using installed 0.2.0 artifacts | Passed on Java 21: bundled UI present, login, user management and metadata |
| `scripts/verify_release_artifacts.py` | Passed for all five library modules; classifiers, UI references, migrations and Java 17 bytecode |
| Strict MkDocs build | Passed |
| Locked frontend dependency audit after Vite 6.4.5 update | 0 reported vulnerabilities at preparation time |

The initial artifact review found missing classifiers for the non-Java modules; release packaging now attaches the React source/configuration archive, starter POM sources, and module HTML documentation. Vite was updated from 5.4 to the compatible patched 6.4 maintenance line; the final bundle was rebuilt and retested. Dependency audit results are time-dependent and do not constitute a full application security review.

Publication verification completed for source commit `2678805`:

- [Java 17/21/25 build and browser CI](https://github.com/substring-projects/jpa-admin/actions/runs/41411406645): passed.
- [H2/PostgreSQL/MySQL contract CI](https://github.com/substring-projects/jpa-admin/actions/runs/41411406624): passed.
- GPG-signed artifacts uploaded and validated by Central. The first validation found a missing identity project name; commit `2678805` corrected it and added a packaging assertion.
- Deployment `81493537-1a19-481a-80ef-e2e0fece6a81`: PUBLISHED.
- All 21 public POM/JAR artifacts matched the uploaded bundle's SHA-256 hashes.
- Starter and optional identity resolved transitively from Maven Central using an empty local repository.

The release tag `v0.2.0` identifies the artifact source commit. Later documentation-only commits record publication status without changing those artifacts.

## Final publication steps

1. Review and commit the prepared release changes. Keep local IDE launch files and credentials out of the release commit.
2. Run the signed verification in a terminal that can unlock the maintainer's GPG key:

   ```sh
   export GPG_TTY=$(tty)
   ./mvnw -Prelease verify
   ```

3. Review source/Javadoc/signature artifacts and the release diff, then upload with `./mvnw -Prelease deploy`. This is a publishing action, not a dry run. The profile uses `autoPublish=false` and excludes the sample.
4. In Central Portal, review the deployment and publish. Confirm both starter and optional identity are publicly resolvable at 0.2.0 from an empty Maven cache; resolving the starter alone does not fetch identity.
5. Create the release tag for the exact reviewed commit and publish release notes from [CHANGELOG](../CHANGELOG.md). Do not tag an unreviewed working tree.
6. Remove publication-pending notices only after availability is confirmed, then publish the docs. Move ongoing development to a new snapshot version after the release.

The local preparation stage did not publish. Signing, CI verification, Central validation and public availability were completed separately as recorded above.
