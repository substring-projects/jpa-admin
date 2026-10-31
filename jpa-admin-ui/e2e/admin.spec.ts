import { expect, test, type Page } from '@playwright/test';

async function signIn(page: Page) {
  await page.goto('./');
  await page.getByLabel('Username').fill('admin');
  await page.getByLabel('Password', { exact: true }).fill('admin');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('heading', { level: 1 })).toContainText(/Good (morning|afternoon|evening)/);
}

const errors: string[] = [];
test.beforeEach(async ({ page }) => {
  errors.length = 0;
  page.on('pageerror', err => errors.push(err.message));
  // Expected HTTP failures are asserted through the UI; the browser's own "Failed to load resource" log is noise.
  page.on('console', msg => { if (msg.type() === 'error' && !msg.text().startsWith('Failed to load resource')) errors.push(msg.text()); });
  await signIn(page);
});
test.afterEach(() => expect(errors, 'browser console errors').toEqual([]));

test('dashboard shows record counts and recent activity', async ({ page }) => {
  await expect(page.getByText('Total records')).toBeVisible();
  await expect(page.getByText('Records created')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Products' }).first()).toBeVisible();
  await page.getByRole('button', { name: 'Show as table' }).click();
  await expect(page.getByRole('columnheader', { name: 'Total' })).toBeVisible();
});

test('list state lives in the URL and survives a reload', async ({ page }) => {
  await page.goto('Product?q=pro&sort=-price');
  await expect(page.getByLabel('Search products')).toHaveValue('pro');
  const firstRow = page.locator('tbody tr[data-row-id]').first();
  await expect(firstRow).toContainText('MacBook Pro');
  await page.reload();
  await expect(page.locator('tbody tr[data-row-id]').first()).toContainText('MacBook Pro');
});

test('relation and range filters narrow the table and show readable chips', async ({ page }) => {
  await page.goto('Product?f.category=1&f.price__gte=1000');
  await expect(page.getByText('Category:')).toBeVisible();
  await expect(page.getByText('Electronics', { exact: true }).first()).toBeVisible();
  await expect(page.getByText('≥ 1,000')).toBeVisible();
  await expect(page.locator('tbody tr[data-row-id]')).toHaveCount(2);
  await page.getByRole('button', { name: 'Clear all' }).click();
  await expect(page).toHaveURL(/\/Product$/);
});

test('saved views restore filters', async ({ page }) => {
  await page.goto('Product?f.status=PUBLISHED');
  await page.getByRole('button', { name: 'Views' }).click();
  await page.getByRole('menuitem', { name: 'Save current view…' }).click();
  await page.getByLabel('Name').fill('Live products');
  await page.getByRole('button', { name: 'Save view' }).click();
  await page.goto('Product');
  await page.getByRole('button', { name: 'Views' }).click();
  await page.getByRole('menuitem', { name: 'Live products' }).click();
  await expect(page).toHaveURL(/f\.status=PUBLISHED/);
});

test('validation errors appear next to the field and save failures explain themselves', async ({ page }) => {
  await page.goto('Category/1/edit');
  const name = page.locator('[id="field-name"]');
  await name.fill('');
  await page.getByRole('button', { name: 'Save changes' }).click();
  await expect(page.locator('[id="field-name-error"]')).toHaveText('Category Name is required');
  await expect(name).toBeFocused();
  // A duplicate of a unique column is only caught by the database.
  await name.fill('Fashion & Apparel');
  await page.getByRole('button', { name: 'Save changes' }).click();
  await expect(page.getByText('The record could not be saved')).toBeVisible();
  await expect(page.getByText(/conflicts with existing data/)).toBeVisible();
});

test('cells can be edited in place', async ({ page }) => {
  await page.goto('Product');
  const row = page.locator('tbody tr[data-row-id="3"]');
  await row.hover();
  await row.getByRole('button', { name: 'Edit Stock Qty' }).click();
  const input = page.getByRole('dialog').locator('input');
  await input.fill('81');
  await page.getByRole('dialog').getByRole('button', { name: 'Save' }).click();
  await expect(row).toContainText('81');
});

test('inline rows are edited and saved with the parent', async ({ page }) => {
  await page.goto('Order/1/edit');
  await expect(page.getByText('Order Line Items')).toBeVisible();
  const quantity = page.locator('[id="field-items[0].quantity"]');
  await quantity.fill('0');
  await page.getByRole('button', { name: 'Save changes' }).click();
  await expect(page.locator('[id="field-items[0].quantity-error"]')).toBeVisible();
  await quantity.fill('3');
  await page.getByRole('button', { name: 'Save changes' }).click();
  await expect(page).toHaveURL(/\/Order\/1$/);
  await page.getByRole('tab', { name: /Order Line Items/ }).click();
  await expect(page.getByRole('tabpanel').getByRole('cell', { name: '3', exact: true })).toBeVisible();
});

test('many-to-many tags are chosen with a multi-select', async ({ page }) => {
  await page.goto('Product/4/edit');
  await page.locator('[id="field-tags"]').click();
  await page.getByRole('option', { name: 'Premium' }).click();
  await page.keyboard.press('Escape');
  await page.getByRole('button', { name: 'Save changes' }).click();
  await expect(page).toHaveURL(/\/Product\/4$/);
  await expect(page.getByRole('link', { name: 'Premium' })).toBeVisible();
});

test('keyboard shortcuts navigate the list', async ({ page }) => {
  await page.goto('Product');
  await expect(page.locator('tbody tr[data-row-id]').first()).toBeVisible();
  await page.keyboard.press('j');
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/\/Product\/\d+$/);
  await expect(page.getByRole('link', { name: 'Edit' })).toBeVisible();
  await page.keyboard.press('e');
  await expect(page).toHaveURL(/\/edit$/);
  await page.goto('Product');
  await page.keyboard.press('?');
  await expect(page.getByRole('heading', { name: 'Keyboard shortcuts' })).toBeVisible();
});

test('opening a form with rich text does not mark it as changed', async ({ page }) => {
  await page.goto('Product/1/edit');
  await expect(page.getByRole('toolbar', { name: 'Formatting' })).toBeVisible();
  await expect(page.locator('.cm-editor')).toBeVisible();
  await expect(page.getByText('Unsaved changes')).toHaveCount(0);
});

test('rich text is sanitized when displayed', async ({ page }) => {
  await page.goto('Product/1');
  await expect(page.getByRole('heading', { name: 'Built for pros' })).toBeVisible();
  await expect(page.locator('script')).toHaveCount(await page.locator('head script').count());
});
