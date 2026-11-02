import { expect, test } from '@playwright/test';

test('invalid login, successful login, reload and logout protect direct API access', async ({ page }) => {
  await page.goto('./');
  await expect(page.getByRole('button', { name: 'Sign in', exact: true })).toBeVisible();
  expect((await page.request.get('api/metadata')).status()).toBe(401);
  await page.getByLabel('Username').fill('admin');
  await page.getByLabel('Password', { exact: true }).fill('incorrect-password');
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.getByText('Invalid username or password.')).toBeVisible();
  await expect(page.getByLabel('Password', { exact: true })).toHaveValue('');
  await page.getByLabel('Password', { exact: true }).fill('admin');
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.getByRole('heading', { level: 1 })).toContainText(/Good (morning|afternoon|evening)/);
  await page.reload();
  await expect(page.getByRole('heading', { level: 1 })).toContainText(/Good (morning|afternoon|evening)/);
  expect((await page.request.get('api/metadata')).status()).toBe(200);
  await page.getByRole('button', { name: /admin.*Administrator/i }).click();
  await page.getByRole('menuitem', { name: 'Sign out' }).click();
  await expect(page.getByRole('button', { name: 'Sign in', exact: true })).toBeVisible();
  expect((await page.request.get('api/metadata')).status()).toBe(401);
  await page.goto('Product');
  await expect(page.getByRole('button', { name: 'Sign in', exact: true })).toBeVisible();
});

test('real HTTP session requires CSRF and rejects a pre-login token after login', async ({ request }) => {
  const before = await (await request.get('api/auth/session')).json();
  expect(before.authenticated).toBe(false);
  expect((await request.post('api/auth/login', { form: { username: 'admin', password: 'admin' } })).status()).toBe(403);
  expect((await request.post('api/auth/login', {
    form: { username: 'admin', password: 'admin' },
    headers: { [before.csrfHeader]: before.csrfToken },
  })).status()).toBe(204);
  const after = await (await request.get('api/auth/session')).json();
  expect(after.authenticated).toBe(true);
  expect((await request.post('api/auth/logout')).status()).toBe(403);
  expect((await request.post('api/auth/logout', { headers: { [before.csrfHeader]: before.csrfToken } })).status()).toBe(403);
  expect((await request.get('api/metadata')).status()).toBe(200);
  expect((await request.post('api/auth/logout', { headers: { [after.csrfHeader]: after.csrfToken } })).status()).toBe(204);
  expect((await request.get('api/Product')).status()).toBe(401);
  expect((await (await request.get('api/auth/session')).json()).authenticated).toBe(false);
});

test('losing the session returns the UI to sign-in', async ({ page, context }) => {
  await page.goto('./');
  await page.getByLabel('Username').fill('admin');
  await page.getByLabel('Password', { exact: true }).fill('admin');
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.getByRole('heading', { level: 1 })).toContainText(/Good (morning|afternoon|evening)/);
  await context.clearCookies();
  await page.getByRole('link', { name: 'Products', exact: true }).first().click();
  await expect(page.getByRole('button', { name: 'Sign in', exact: true })).toBeVisible();
  await expect(page.getByText('Your session has expired. Please sign in again.')).toBeVisible();
});
