# Development

[Documentation index](README.md) · [Project overview](../README.md)

Applies to `1.0.0-SNAPSHOT` (Spring Boot 4.0.0).

## Frontend development

Run the sample backend on port **8080**, then start Vite in a separate terminal:

```bash
cd jpa-admin-ui
npm ci
npm run dev
```

Open **http://localhost:5173/admin/**. Vite proxies `/admin/api` to `http://localhost:8080`, including login requests. Access the UI and its API through the same browser origin so the session cookie and CSRF token stay together.

If the backend uses another port, update the proxy target in [vite.config.ts](../jpa-admin-ui/vite.config.ts). `npm run preview` previews built assets; it is not a replacement for running the backend and configuring API access.

### UI source map

| Location                              | Purpose                                                     |
| ------------------------------------- | ----------------------------------------------------------- |
| `src/views/LoginView.tsx`             | Login form, pending state, and errors.                      |
| `src/context/AuthContext.tsx`         | Session bootstrap and authentication gate.                  |
| `src/api/auth.ts`                     | Login/logout, CSRF token handling, and authenticated fetch. |
| `src/context/AdminContext.tsx`        | Metadata, navigation, theme, and notifications.             |
| `src/components/Sidebar.tsx`          | Grouped navigation, search, collapse, and account footer.   |
| `src/views/EntityListView.tsx`        | Tables, filters, pagination, selection, and actions.        |
| `src/views/EntityFormView.tsx`        | Metadata-driven create/edit forms.                          |
| `src/components/RelationSelect.tsx`   | Async relation search and selection.                        |
| `src/components/LoadingSkeletons.tsx` | Dashboard, form, and table loading placeholders.            |
| `src/components/ui/`                  | Shared local UI primitives.                                 |
| `src/index.css`                       | Tailwind CSS 4 theme tokens, colors, and animations.        |
| `src/lib/form-data.ts`                | Writable-field selection and payload construction.          |

The `@/` alias resolves to `src/`. [components.json](../jpa-admin-ui/components.json) configures the shadcn CLI. Tailwind runs through the Vite plugin; the older `tailwind.config.js` is not the active source of theme tokens. Review generated component changes before replacing customized primitives.

## Build and test

From the repository root:

```bash
# Frontend regression tests.
npm test --prefix jpa-admin-ui

# TypeScript check and production frontend bundle.
npm run build --prefix jpa-admin-ui

# Java tests across the reactor; also invokes the frontend build.
./mvnw test

# Package all modules and the runnable sample JAR.
./mvnw package

# Install artifacts into the local Maven repository.
./mvnw install
```

Focused Maven checks:

```bash
# Core and authentication module tests.
./mvnw -pl jpa-admin-spring-boot-autoconfigure -am test

# Build the UI resource JAR.
./mvnw -pl jpa-admin-ui -am package
```

Current automated coverage includes:

- Preserving unchanged sensitive values and excluding protected fields from frontend payloads.
- Preserving `0`, `false`, explicit `null`, and manually assigned create IDs.
- Excluding unsupported nested fields from scalar forms.
- Blocking anonymous metadata/record requests.
- Authenticating with configured credentials and invalidating sessions on logout.
- Requiring CSRF tokens for login and authenticated writes.

These are focused regression tests, not a complete end-to-end or cross-browser test suite. There is no dedicated lint script or CI workflow in this checkout. If Vite reports a large-chunk warning, review the build exit status separately; a warning alone is not a build failure.
