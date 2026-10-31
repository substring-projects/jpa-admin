import { defineConfig, devices } from '@playwright/test';

const port = Number(process.env.E2E_PORT || 8090);
const jar = '../jpa-admin-sample-app/target/jpa-admin-sample-app-1.0.0-SNAPSHOT.jar';

/**
 * End-to-end tests against the packaged sample app (build it first with `./mvnw install`).
 * Set E2E_BASE_URL to test an already running server instead; E2E_CHANNEL=chromium uses Playwright's bundled browser.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL || `http://localhost:${port}/admin/`,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{
    name: 'desktop',
    use: { ...devices['Desktop Chrome'], channel: process.env.E2E_CHANNEL ?? (process.env.CI ? 'chromium' : 'chrome') },
  }],
  webServer: process.env.E2E_BASE_URL ? undefined : {
    command: `java -jar ${jar} --server.port=${port} --spring.jpa.admin.security.password=admin`,
    url: `http://localhost:${port}/admin/api/auth/session`,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
