import { expect, test, type Page } from '@playwright/test';

test.skip(process.env.E2E_SECURITY_MODE !== 'database', 'Requires the opt-in database identity store');
async function signIn(page: Page, username: string, password: string) {
  await page.goto('./');
  await page.getByLabel('Username', { exact: true }).fill(username);
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.getByRole('heading', { level: 1 })).toContainText(/Good (morning|afternoon|evening)/);
}

test('superuser manages a group and user; reader sees only permitted records and loses access after revocation', async ({ page, browser, baseURL }) => {
  const suffix = Date.now();
  const groupName = `Product readers ${suffix}`;
  const username = `reader-${suffix}`;
  await signIn(page, 'admin', 'admin');
  await page.getByRole('link', { name: 'Users and groups' }).click();
  await page.getByRole('tab', { name: 'Groups', exact: true }).click();
  await page.getByRole('button', { name: 'Add group', exact: true }).click();
  await page.getByLabel('Group name', { exact: true }).fill(groupName);
  await page.getByRole('checkbox', { name: 'Product view', exact: true }).check();
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByRole('dialog')).not.toBeVisible();
  await page.getByRole('tab', { name: 'Users', exact: true }).click();
  await page.getByRole('button', { name: 'Add user', exact: true }).click();
  await page.getByLabel('Username', { exact: true }).fill(username);
  await page.getByLabel('Password', { exact: true }).fill('reader-password-123');
  await page.getByRole('checkbox', { name: groupName, exact: true }).check();
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByRole('dialog')).not.toBeVisible();

  const context = await browser.newContext({ baseURL });
  try {
    const reader = await context.newPage();
    await signIn(reader, username, 'reader-password-123');
    await expect(reader.getByRole('link', { name: 'Products', exact: true }).first()).toBeVisible();
    await expect(reader.getByRole('link', { name: 'Customers', exact: true })).toHaveCount(0);
    await expect(reader.getByRole('link', { name: 'Users and groups' })).toHaveCount(0);
    await reader.getByRole('link', { name: 'Products', exact: true }).first().click();
    await expect(reader.locator('tbody tr[data-row-id]').first()).toBeVisible();
    await expect(reader.getByRole('link', { name: 'New Product', exact: true })).toHaveCount(0);
    const session = await (await reader.request.get('api/auth/session')).json();
    const denied = await reader.request.put('api/Product/1', {
      headers: { [session.csrfHeader]: session.csrfToken }, data: { name: 'Unauthorized edit' },
    });
    expect(denied.status()).toBe(403);
    expect((await denied.json()).code).toBe('ACCESS_DENIED');

    await page.getByRole('tab', { name: 'Groups', exact: true }).click();
    await page.getByRole('button', { name: `Edit group ${groupName}`, exact: true }).click();
    await page.getByRole('checkbox', { name: 'Product view', exact: true }).uncheck();
    await page.getByRole('button', { name: 'Save', exact: true }).click();
    await expect(page.getByRole('dialog')).not.toBeVisible();
    expect((await reader.request.get('api/Product')).status()).toBe(403);
    await reader.reload();
    await expect(reader.getByRole('link', { name: 'Products', exact: true })).toHaveCount(0);
  } finally { await context.close(); }
});

test('UI displays the last-superuser protection and keeps the account', async ({ page }) => {
  await signIn(page, 'admin', 'admin');
  await page.getByRole('link', { name: 'Users and groups' }).click();
  await page.getByRole('button', { name: 'Edit user admin', exact: true }).click();
  await page.getByRole('checkbox', { name: 'Active account', exact: true }).uncheck();
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('At least one active superuser');
  await page.getByRole('button', { name: 'Cancel', exact: true }).click();
  expect((await (await page.request.get('api/auth/session')).json()).authenticated).toBe(true);
});


test('permission matrix keeps View dependencies and supports entity search and bulk selection', async ({ page }) => {
  await signIn(page, 'admin', 'admin');
  await page.getByRole('link', { name: 'Users and groups' }).click();
  await page.getByRole('tab', { name: 'Groups', exact: true }).click();
  await page.getByRole('button', { name: 'Add group', exact: true }).click();
  await page.getByRole('checkbox', { name: 'Product change', exact: true }).check();
  await expect(page.getByRole('checkbox', { name: 'Product view', exact: true })).toBeChecked();
  await page.getByRole('checkbox', { name: 'Product view', exact: true }).uncheck();
  await expect(page.getByRole('checkbox', { name: 'Product change', exact: true })).not.toBeChecked();
  await page.getByLabel('Search entities').fill('Product');
  await expect(page.getByRole('checkbox', { name: 'Customer view', exact: true })).toHaveCount(0);
  await page.getByRole('checkbox', { name: 'Select all Product permissions', exact: true }).check();
  await expect(page.getByRole('checkbox', { name: 'Product delete', exact: true })).toBeChecked();
  await page.getByLabel('Search entities').fill('no-such-entity');
  await expect(page.getByText('No entities found.', { exact: true })).toBeVisible();
  await page.getByLabel('Search entities').fill('Product');
  await expect(page.getByRole('checkbox', { name: 'Product view', exact: true })).toBeChecked();
  await page.getByRole('button', { name: 'Cancel', exact: true }).click();
});
