import { test, expect } from '@playwright/test';

// Helper to generate unique email for each test
const uniqueEmail = () => `test-${Date.now()}@example.com`;

// Helper to register a new user
async function register(page, email, password = 'password123', displayName = 'Test User') {
  await page.goto('/register');
  await page.getByTestId('register-email').fill(email);
  await page.getByTestId('register-display-name').fill(displayName);
  await page.getByTestId('register-password').fill(password);
  await Promise.all([
    page.waitForURL(/\/login/),
    page.getByTestId('register-submit').click(),
  ]);
}

// Helper to login
async function login(page, email, password = 'password123') {
  await page.goto('/login');
  await page.getByTestId('login-email').fill(email);
  await page.getByTestId('login-password').fill(password);
  await Promise.all([
    page.waitForURL('/dashboard'),
    page.getByTestId('login-submit').click(),
  ]);
}

// Helper to register and login
async function registerAndLogin(page, email = uniqueEmail(), password = 'password123') {
  await register(page, email, password);
  await login(page, email, password);
  return email;
}

test.describe('Sales Registration', () => {

  test('can register sale with multiple line items for different releases', async ({ page }) => {
    // Setup: Listen for console logs and errors
    const consoleLogs = [];
    const consoleErrors = [];
    page.on('console', msg => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
      consoleLogs.push(`[${msg.type()}] ${msg.text()}`);
    });
    page.on('pageerror', error => {
      consoleErrors.push(error.message);
    });

    // Setup: Login
    await registerAndLogin(page);

    // Create a label
    await page.getByTestId('create-label-button').click();
    await page.getByTestId('label-name-input').fill('Sales Test Label');
    await Promise.all([
      page.waitForURL('/dashboard'),
      page.getByTestId('create-label-submit').click(),
    ]);

    // Create an artist
    await page.getByTestId('create-artist-button').click();
    await page.getByTestId('artist-name-input').fill('Sales Test Artist');
    await Promise.all([
      page.waitForURL('/dashboard'),
      page.getByTestId('create-artist-submit').click(),
    ]);

    // Go to label page
    await page.getByTestId('label-link').filter({ hasText: 'Sales Test Label' }).click();
    await expect(page.locator('h1')).toContainText('Sales Test Label');

    // Create first release
    await page.getByTestId('create-release-button').click();
    await expect(page.getByTestId('create-release-modal')).toBeVisible();
    await page.getByTestId('release-name-input').fill('First Release');
    await page.getByTestId('release-date-input').fill('2024-01-15');
    await page.getByTestId('release-artist-select').selectOption({ label: 'Sales Test Artist' });
    await page.getByTestId('format-vinyl').check();
    await page.getByTestId('track-name-input').fill('Track 1');
    await page.getByTestId('track-duration-input').fill('3:00');
    await page.getByTestId('track-artist-select').selectOption({ label: 'Sales Test Artist' });
    await Promise.all([
      page.waitForURL(/\/labels\/\d+$/),
      page.getByTestId('create-release-submit').click(),
    ]);

    // Verify first release was created
    await expect(page.getByText('First Release')).toBeVisible();

    // Create second release
    await page.getByTestId('create-release-button').click();
    await expect(page.getByTestId('create-release-modal')).toBeVisible();
    await page.getByTestId('release-name-input').fill('Second Release');
    await page.getByTestId('release-date-input').fill('2024-02-20');
    await page.getByTestId('release-artist-select').selectOption({ label: 'Sales Test Artist' });
    await page.getByTestId('format-cd').check();
    await page.getByTestId('track-name-input').fill('Track 2');
    await page.getByTestId('track-duration-input').fill('4:30');
    await page.getByTestId('track-artist-select').selectOption({ label: 'Sales Test Artist' });
    await Promise.all([
      page.waitForURL(/\/labels\/\d+$/),
      page.getByTestId('create-release-submit').click(),
    ]);

    // Verify second release was created
    await expect(page.getByText('Second Release')).toBeVisible();

    // Navigate to sales (look for "View All Sales" link)
    const salesLink = page.locator('a').filter({ hasText: 'View All Sales' });
    await salesLink.click();

    // Should be on sales list page
    await expect(page).toHaveURL(/\/labels\/\d+\/sales$/);

    // Click "Register Sale" or similar button to get to registration form
    const registerSaleLink = page.locator('a').filter({ hasText: /Register Sale|New Sale/i });
    await registerSaleLink.click();

    // Should be on sale registration form
    await expect(page).toHaveURL(/\/labels\/\d+\/sales\/new$/);
    await expect(page.locator('h1')).toContainText('Register Sale');

    // Fill basic sale information
    await page.locator('#saleDate').fill('2024-03-15');
    await page.locator('#channel').selectOption('EVENT');
    await page.locator('#notes').fill('Concert sale test');

    // Verify first line item exists (should be rendered by Thymeleaf)
    const firstLineItem = page.locator('.line-item').first();
    await expect(firstLineItem).toBeVisible();

    // Verify first line item has releases in dropdown
    const firstReleaseSelect = firstLineItem.locator('select.release-select');
    await expect(firstReleaseSelect).toBeVisible();

    // Check that releases are available
    const firstReleaseOptions = await firstReleaseSelect.locator('option').allTextContents();
    console.log('First line item release options:', firstReleaseOptions);
    expect(firstReleaseOptions).toContain('First Release');
    expect(firstReleaseOptions).toContain('Second Release');

    // Fill first line item
    await firstReleaseSelect.selectOption({ label: 'First Release' });
    await firstLineItem.locator('select.format-select').selectOption('VINYL');
    await firstLineItem.locator('input.quantity-input').fill('5');
    await firstLineItem.locator('input.price-input').fill('15.00');

    // Wait a bit for module to load
    await page.waitForTimeout(1000);

    // Click "Add Line Item" button
    const addLineItemButton = page.locator('button').filter({ hasText: '+ Add Line Item' });
    await expect(addLineItemButton).toBeVisible();

    console.log('Clicking Add Line Item button...');

    // Try calling the function directly to see if it works
    const directCallResult = await page.evaluate(() => {
      try {
        const container = document.getElementById('lineItemsContainer');
        const beforeCount = container?.querySelectorAll('.line-item').length || 0;

        // Note: SaleForm is in module scope, not on window
        // window.SaleForm.addLineItem();

        const afterCount = container?.querySelectorAll('.line-item').length || 0;
        return { beforeCount, afterCount, containerExists: !!container };
      } catch (error) {
        return { error: error.message };
      }
    });
    console.log('Direct call result:', directCallResult);

    await addLineItemButton.click();

    // Wait a bit for JavaScript to execute
    await page.waitForTimeout(500);

    // Log console messages if anything went wrong
    if (consoleLogs.length > 0) {
      console.log('Browser console logs:', consoleLogs);
    }
    if (consoleErrors.length > 0) {
      console.log('Browser console errors:', consoleErrors);
    }

    // Verify second line item appears
    const lineItems = page.locator('.line-item');
    await expect(lineItems).toHaveCount(2);

    const secondLineItem = lineItems.nth(1);
    await expect(secondLineItem).toBeVisible();

    // Verify second line item has releases in dropdown
    const secondReleaseSelect = secondLineItem.locator('select.release-select');
    await expect(secondReleaseSelect).toBeVisible();

    // Check that releases are available in second line item
    const secondReleaseOptions = await secondReleaseSelect.locator('option').allTextContents();
    console.log('Second line item release options:', secondReleaseOptions);
    expect(secondReleaseOptions).toContain('First Release');
    expect(secondReleaseOptions).toContain('Second Release');

    // Fill second line item
    await secondReleaseSelect.selectOption({ label: 'Second Release' });
    await secondLineItem.locator('select.format-select').selectOption('CD');
    await secondLineItem.locator('input.quantity-input').fill('10');
    await secondLineItem.locator('input.price-input').fill('12.00');

    // Submit the form
    const submitButton = page.locator('button[type="submit"]').filter({ hasText: 'Register Sale' });
    await submitButton.click();

    // Should redirect to sales list or detail page
    await page.waitForURL(/\/labels\/\d+\/sales/, { timeout: 5000 });

    // Verify sale was created (should show in list or detail)
    await expect(page.locator('body')).toContainText(/Concert sale test|First Release|Second Release/);
  });

  test('first line item cannot be removed', async ({ page }) => {
    // Setup: Login and create label with releases
    await registerAndLogin(page);

    await page.getByTestId('create-label-button').click();
    await page.getByTestId('label-name-input').fill('Remove Test Label');
    await Promise.all([
      page.waitForURL('/dashboard'),
      page.getByTestId('create-label-submit').click(),
    ]);

    await page.getByTestId('create-artist-button').click();
    await page.getByTestId('artist-name-input').fill('Remove Test Artist');
    await Promise.all([
      page.waitForURL('/dashboard'),
      page.getByTestId('create-artist-submit').click(),
    ]);

    await page.getByTestId('label-link').filter({ hasText: 'Remove Test Label' }).click();

    await page.getByTestId('create-release-button').click();
    await page.getByTestId('release-name-input').fill('Test Release');
    await page.getByTestId('release-date-input').fill('2024-01-15');
    await page.getByTestId('release-artist-select').selectOption({ label: 'Remove Test Artist' });
    await page.getByTestId('format-vinyl').check();
    await page.getByTestId('track-name-input').fill('Track 1');
    await page.getByTestId('track-duration-input').fill('3:00');
    await page.getByTestId('track-artist-select').selectOption({ label: 'Remove Test Artist' });
    await Promise.all([
      page.waitForURL(/\/labels\/\d+$/),
      page.getByTestId('create-release-submit').click(),
    ]);

    // Navigate to sales registration
    await page.locator('a').filter({ hasText: 'View All Sales' }).click();
    await page.locator('a').filter({ hasText: /Register Sale|New Sale/i }).click();

    // Verify first line item's remove button is hidden
    const firstLineItem = page.locator('.line-item').first();
    const firstRemoveButton = firstLineItem.locator('button.remove-item');

    // Button should exist but be hidden
    await expect(firstRemoveButton).toBeHidden();

    // Add a second line item
    await page.locator('button').filter({ hasText: '+ Add Line Item' }).click();
    await page.waitForTimeout(500);

    // Second line item's remove button should be visible
    const secondLineItem = page.locator('.line-item').nth(1);
    const secondRemoveButton = secondLineItem.locator('button.remove-item');
    await expect(secondRemoveButton).toBeVisible();

    // Click remove on second item
    await secondRemoveButton.click();
    await page.waitForTimeout(500);

    // Should only have one line item now
    await expect(page.locator('.line-item')).toHaveCount(1);

    // First item's remove button should still be hidden
    await expect(firstRemoveButton).toBeHidden();
  });

});
