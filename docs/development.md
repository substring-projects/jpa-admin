# Development

<p class="jpa-lead">Build, test and change the project locally.</p>

## Setup

| Tool | Version |
| --- | --- |
| JDK | 17+ |
| Node.js / npm | 20+ |
| Maven | Wrapper included (`./mvnw`) |
| Python | 3.12+ for the documentation site (optional) |
| Google Chrome | For the browser tests (or Playwright's bundled Chromium) |

```bash
npm ci --prefix jpa-admin-ui
./mvnw clean install          # builds the UI, runs all Java tests, installs the modules
```

## Frontend development

Start the sample backend, then Vite with hot reload:

```bash
java -jar jpa-admin-sample-app/target/jpa-admin-sample-app-0.2.0-SNAPSHOT.jar   # terminal 1
npm run dev --prefix jpa-admin-ui                                                 # terminal 2
```

Open **<http://localhost:5173/admin/>**. Vite proxies `/admin/api` to port 8080, so sessions and CSRF work. To use another backend port, change the proxy in [`vite.config.ts`](../jpa-admin-ui/vite.config.ts).

- The `@/` import alias points to `src/`.
- Add shadcn components with `npx shadcn@latest add <name>` (configured in [`components.json`](../jpa-admin-ui/components.json)). Check the generated imports use `@/lib/utils`.
- Theme tokens live in `src/index.css` (Tailwind CSS 4, oklch colors, light and dark).
- See [Architecture](architecture.md#in-the-browser) for the source layout.

## Tests

| Command | What it covers |
| --- | --- |
| `./mvnw test` | Java tests: security, CRUD features (filters, validation, inline rows, many-to-many, timeline), uploads (local flow, rejections, forged tickets, S3 and ImageKit against simulated servers), and path validation. |
| `npm test --prefix jpa-admin-ui` | UI unit tests: payload building, formatting, validation. |
| `npm run test:e2e --prefix jpa-admin-ui` | Playwright browser tests against the packaged sample app: login, URL state, filters, saved views, validation, edit-in-place, inline rows, many-to-many, shortcuts, rich text, uploads (progress, rejection, cancel) and the record media panel. Each test also fails on browser console errors. |

The browser tests start the sample JAR on port 8090 using your installed Chrome. Build first with `./mvnw install`. Useful variables:

```bash
E2E_CHANNEL=chromium npm run test:e2e --prefix jpa-admin-ui      # Playwright's Chromium (npx playwright install chromium)
E2E_BASE_URL=http://localhost:8080/backoffice/ npm run test:e2e --prefix jpa-admin-ui   # an already running server
```

## Useful Maven commands

```bash
./mvnw -pl jpa-admin-core test                       # one module
./mvnw -q install -DskipTests -DskipNpmBuild=true    # Java only, reuse the committed UI build
./mvnw -pl jpa-admin-ui,jpa-admin-sample-app install -DskipTests   # rebuild the UI and the sample JAR
```

## Documentation

```bash
python3 -m venv .venv-docs && .venv-docs/bin/pip install -r requirements-docs.txt
.venv-docs/bin/mkdocs serve          # http://127.0.0.1:8000/jpa-admin/
.venv-docs/bin/mkdocs build --strict # what CI runs
```

See [Documentation website](publishing-docs.md) for the theme and publishing.

## Local development version

The working version is `0.2.0-SNAPSHOT`; the published `0.1.0` remains unchanged.
Run `./mvnw clean install` from the repository root to build and test all modules
and install them into your local Maven repository. No release profile, GPG signing
or publishing token is needed.

A separate local application can use:

```xml
<dependency>
    <groupId>com.substringtechnologies</groupId>
    <artifactId>jpa-admin-spring-boot-starter</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

After changing the library, run `./mvnw install` again, then rebuild and restart
the consuming application. Reuse this snapshot version during development.
Keep public installation examples on `0.1.0` until the next release is published.
