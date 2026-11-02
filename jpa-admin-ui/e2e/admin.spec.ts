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

test('slider widgets save their value and computed fields use their format', async ({ page }) => {
  await page.goto('Product/2/edit');
  const slider = page.getByRole('slider', { name: 'Discount (%)' });
  await slider.focus();
  await page.keyboard.press('Home');
  await page.keyboard.press('ArrowRight');
  await page.keyboard.press('ArrowRight');
  await expect(slider).toHaveAttribute('aria-valuenow', '10');
  await expect(page.locator('[id="field-stockQuantity"]')).toHaveAttribute('min', '0');
  await page.getByRole('button', { name: 'Save changes' }).click();
  await expect(page).toHaveURL(/\/Product\/2$/);
  await expect(page.locator('dt', { hasText: 'Discount (%)' }).locator('xpath=following-sibling::dd')).toHaveText('10');
  await expect(page.locator('dt', { hasText: 'Inventory Value' }).locator('xpath=following-sibling::dd')).toHaveText(/^\$[\d,]+\.\d{2}$/);
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

const PNG_1PX = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==', 'base64');

test.describe('file uploads', () => {
  test('an image uploads with progress, previews, and is saved on the record', async ({ page }) => {
    await page.goto('Product/5/edit');
    // Slow the transfer slightly so the progress state is observable.
    await page.route('**/api/uploads/local/**', async route => { await new Promise(r => setTimeout(r, 600)); await route.continue(); });
    await page.getByRole('button', { name: /Upload Image/ }).waitFor();
    await page.locator('input[type=file]').first().setInputFiles({ name: 'espresso.png', mimeType: 'image/png', buffer: PNG_1PX });
    await expect(page.getByRole('progressbar', { name: 'Uploading espresso.png' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Waiting for upload…' })).toBeDisabled();
    await expect(page.getByText('Uploaded')).toBeVisible();
    await page.getByRole('button', { name: 'Save changes' }).click();
    await expect(page).toHaveURL(/\/Product\/5$/);
    const image = page.getByRole('button', { name: /View image full size/ }).locator('img');
    await expect(image).toHaveAttribute('src', /^\/uploads\/products\/\d{4}\/\d{2}\/[0-9a-f]{8}-espresso\.png$/);
    await expect.poll(() => image.evaluate((img: HTMLImageElement) => img.naturalWidth)).toBe(1);
  });

  test('the detail page shows the image in a media panel with a full-size viewer', async ({ page }) => {
    await page.goto('Product/3');
    const image = page.getByRole('button', { name: /View image full size/ }).locator('img');
    await expect.poll(() => image.evaluate((img: HTMLImageElement) => img.getBoundingClientRect().width)).toBeGreaterThan(200);
    await page.getByRole('button', { name: /View image full size/ }).click();
    const viewer = page.getByRole('dialog');
    await expect(viewer.getByRole('heading')).toHaveText('Sony WH-1000XM5 Noise Canceling · Image');
    await expect(viewer.getByRole('link', { name: 'Download' })).toHaveAttribute('href', '/images/products/headphones.svg');
    await page.keyboard.press('Escape');
    await expect(viewer).toBeHidden();
  });

  test('a record without an image offers an upload shortcut', async ({ page }) => {
    await page.goto('Product/new');
    await page.getByLabel('Product Title').fill('Imageless kettle');
    await page.locator('[id="field-price"]').fill('49');
    await page.locator('[id="field-stockQuantity"]').fill('3');
    await page.locator('[id="field-status"]').click();
    await page.getByRole('option', { name: 'Draft' }).click();
    await page.getByRole('button', { name: 'Create product' }).click();
    await expect(page.getByText('No image yet')).toBeVisible();
    await page.getByRole('link', { name: 'Upload image' }).click();
    await expect(page).toHaveURL(/\/edit$/);
  });

  test('documents upload as file links', async ({ page }) => {
    await page.goto('Product/3/edit');
    const pdf = Buffer.from('%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n');
    await page.locator('input[type=file]').nth(1).setInputFiles({ name: 'datasheet.pdf', mimeType: 'application/pdf', buffer: pdf });
    await expect(page.getByText('Uploaded')).toBeVisible();
    await page.getByRole('button', { name: 'Save changes' }).click();
    await expect(page).toHaveURL(/\/Product\/3$/);
    await expect(page.getByRole('link', { name: /\.pdf$/ })).toBeVisible();
  });

  test('wrong types and oversized files are refused before uploading', async ({ page }) => {
    await page.goto('Product/5/edit');
    const input = page.locator('input[type=file]').first();
    await input.setInputFiles({ name: 'notes.txt', mimeType: 'text/plain', buffer: Buffer.from('hello') });
    await expect(page.getByRole('alert').filter({ hasText: "notes.txt isn't an allowed file type" })).toBeVisible();
    await input.setInputFiles({ name: 'huge.png', mimeType: 'image/png', buffer: Buffer.alloc(6 * 1024 * 1024) });
    await expect(page.getByRole('alert').filter({ hasText: 'the limit is 5 MB' })).toBeVisible();
  });

  test('a file disguised as an image is rejected by the server', async ({ page }) => {
    await page.goto('Product/5/edit');
    await page.locator('input[type=file]').first().setInputFiles({ name: 'cat.png', mimeType: 'image/png', buffer: Buffer.from('<!DOCTYPE html><script>alert(1)</script>') });
    await expect(page.getByRole('alert').filter({ hasText: "content doesn't match its type" })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Retry' })).toBeVisible();
  });

  test('a slow upload can be cancelled', async ({ page }) => {
    await page.goto('Product/new');
    await page.route('**/api/uploads/local/**', async route => { await new Promise(r => setTimeout(r, 5000)); await route.continue().catch(() => undefined); });
    await page.locator('input[type=file]').first().setInputFiles({ name: 'slow.png', mimeType: 'image/png', buffer: PNG_1PX });
    await page.getByRole('button', { name: 'Cancel upload' }).click();
    await expect(page.getByRole('button', { name: /Upload Image/ })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create product' })).toBeEnabled();
  });
});
