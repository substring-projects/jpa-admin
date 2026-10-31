# Release guide

<p class="jpa-lead">Build, sign and publish the parent POM and four library modules together. The sample application stays in GitHub and is excluded from Central publication.</p>

The current development version is `0.2.0-SNAPSHOT`. The latest published release is `0.1.0`, the first Maven Central release; older JitPack tags use a separate version sequence. The project uses [Apache License 2.0](../LICENSE).

## Prepare a release

1. Select the release commit. Update the root version and all five module parent references together. Keep library versions aligned.
2. Review configuration, API and compatibility documentation against the release code. Update [What's new](whats-new.md) and installation examples.
3. Run the checks below. Rebuild and review the tracked `jpa-admin-ui/dist` assets; legacy JitPack builds package them directly.
4. Run the sample with disposable data. Check login/logout, record browsing, create/update, relations, uploads and custom actions. Test the starter in a separate consumer application.
5. Keep credentials, private keys and local Maven settings outside Git. Establish a private vulnerability-reporting channel before advertising one.

```bash
npm ci --prefix jpa-admin-ui
npm test --prefix jpa-admin-ui
./mvnw clean install
npm run test:e2e --prefix jpa-admin-ui
.venv-docs/bin/mkdocs build --strict
```

Maven builds the frontend but does not run its unit or browser tests. There is no automatic Maven Central release workflow in this repository.

## One-time publisher setup

- Create a [Central Portal](https://central.sonatype.com/) account and verify ownership of the `com.substringtechnologies` namespace. Account creation and namespace verification are separate steps.
- Generate a GPG signing key and publish its public key to a supported keyserver. Keep the private key and passphrase local. See [Sonatype's signing guide](https://central.sonatype.org/publish/requirements/gpg/).
- Generate a Central publishing user token. Put its username and password in `~/.m2/settings.xml` using the exact server ID `central`:

```xml title="~/.m2/settings.xml (outside the repository)"
<settings>
    <servers>
        <server>
            <id>central</id>
            <username>YOUR_TOKEN_USERNAME</username>
            <password>YOUR_TOKEN_PASSWORD</password>
        </server>
    </servers>
</settings>
```

If the file already exists, merge the server entry into its existing `servers` section. Use token credentials, not your account password or GPG passphrase. On macOS/Linux, restrict file access with `chmod 600 ~/.m2/settings.xml`.

## Verify signing without uploading

Run from the repository root, where `pom.xml` defines the `release` profile:

```bash
# macOS/Linux: let GPG prompt in this terminal
export GPG_TTY=$(tty)
./mvnw -Prelease verify
```

The profile attaches source and Javadoc JARs and signs release files. Enter your GPG passphrase in the local prompt. This command does not upload artifacts.

## Upload and publish

```bash
./mvnw -Prelease deploy
```

The Central plugin uploads the artifacts for validation. `autoPublish=false` means the maintainer then opens **Central Portal → Publishing → Deployments**, reviews the validated deployment, and clicks **Publish**. Wait for **PUBLISHED**; do not redeploy the same version while it is **PUBLISHING**.

Expected publication:

| Artifact | Packaging |
| --- | --- |
| `jpa-admin-parent` | POM |
| `jpa-admin-core` | JAR |
| `jpa-admin-ui` | JAR containing compiled UI assets |
| `jpa-admin-spring-boot-autoconfigure` | JAR |
| `jpa-admin-spring-boot-starter` | JAR with dependency metadata |

The sample is excluded through `excludeArtifacts`. Verify that it is absent from the Portal deployment. See the [Central Maven plugin guide](https://central.sonatype.org/publish/publish-portal-maven/) for the publishing lifecycle.

Published versions are immutable. For a later fix, choose a new version such as `0.1.1` and update the parent and all module references together.

## Verify public availability

A successful upload or a local `mvn install` does not establish public availability. Check the [public starter POM](https://repo.maven.apache.org/maven2/com/substringtechnologies/jpa-admin-spring-boot-starter/0.1.0/jpa-admin-spring-boot-starter-0.1.0.pom), then resolve the starter and dependencies using an empty cache:

```bash
# macOS/Linux; run outside the project to avoid its reactor and profiles
cd /tmp
release_cache=$(mktemp -d)
mvn org.apache.maven.plugins:maven-dependency-plugin:3.8.1:get \
  -Dartifact=com.substringtechnologies:jpa-admin-spring-boot-starter:0.1.0 \
  -Dmaven.repo.local="$release_cache" \
  -Dtransitive=true
```

After resolution succeeds, test an independent Spring Boot 4 application, record the results, and remove any pending-publication notice from [Installation](installation.md). Create a release tag only for the reviewed source commit matching the published artifacts.

## Common publishing errors

| Error | Resolution |
| --- | --- |
| No POM / profile `release` does not exist | Change into the repository root before running Maven. |
| Cannot get publisher server properties / `server` is null | Add a server with literal ID `central` in Maven settings. `${server}` is not a substitute unless explicitly resolved. |
| GPG: `Inappropriate ioctl for device` | Run in your own terminal with `export GPG_TTY=$(tty)` so the passphrase prompt can open. |
| Unauthorized upload | Check the publishing token and verified namespace in the Portal. |
| Public artifact is not found | Wait for publication and public availability; retry cached failures with `-U`. |

## Publish documentation

Commit the reviewed documentation, root README, license, theme overrides and MkDocs configuration. Pushing to `main` triggers `.github/workflows/docs.yml`, which builds with strict validation and deploys to GitHub Pages. See [Documentation website](publishing-docs.md) for initial Pages setup.

Record the release commit, toolchain versions, actual test results, sample smoke-test outcome and independent consumer result in release notes. Do not claim unexecuted checks passed.
