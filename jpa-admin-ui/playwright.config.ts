import { readFileSync } from 'node:fs';
import { defineConfig, devices } from '@playwright/test';

const port = Number(process.env.E2E_PORT || 8090);
const adminPath = process.env.E2E_ADMIN_PATH || '/admin';
const contextPath = process.env.E2E_CONTEXT_PATH || '';
const securityMode = process.env.E2E_SECURITY_MODE || 'configured';
const baseURL = `http://localhost:${port}${contextPath}${adminPath}/`;
// The packaged sample for the checked-out version; set E2E_JAR to test another build.
const version = /<artifactId>jpa-admin-parent<\/artifactId>\s*<version>([^<]+)<\/version>/.exec(readFileSync('../pom.xml', 'utf8'))?.[1];
const jar = process.env.E2E_JAR || `../jpa-admin-sample-app/target/jpa-admin-sample-app-${version}.jar`;

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
    baseURL: process.env.E2E_BASE_URL || baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{
    name: 'desktop',
    use: { ...devices['Desktop Chrome'], channel: process.env.E2E_CHANNEL ?? (process.env.CI ? 'chromium' : 'chrome') },
  }],
  webServer: process.env.E2E_BASE_URL ? undefined : {
    command: `java -jar ${jar} --server.port=${port} --spring.jpa.admin.security.password=admin --spring.jpa.admin.path=${adminPath} --server.servlet.context-path=${contextPath} --spring.jpa.admin.security.mode=${securityMode} --spring.jpa.admin.security.initialize-schema=${securityMode === 'database'}`,
    url: `${baseURL}api/auth/session`,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
