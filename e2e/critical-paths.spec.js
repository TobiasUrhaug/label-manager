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

test.describe('Critical Paths', () => {

  test('user can register and login', async ({ page }) => {
    const email = uniqueEmail();

    // Register
    await page.goto('/register');
    await page.getByTestId('register-email').fill(email);
    await page.getByTestId('register-display-name').fill('New User');
    await page.getByTestId('register-password').fill('password123');
    await Promise.all([
      page.waitForURL(/\/login/),
      page.getByTestId('register-submit').click(),
    ]);

    // Should redirect to login
    await expect(page).toHaveURL(/\/login/);

    // Login
    await page.getByTestId('login-email').fill(email);
    await page.getByTestId('login-password').fill('password123');
    await Promise.all([
      page.waitForURL('/dashboard'),
      page.getByTestId('login-submit').click(),
    ]);

    // Should be on dashboard
    await expect(page).toHaveURL('/dashboard');
    await expect(page.getByTestId('labels-heading')).toBeVisible();
  });

  test('authenticated user can create and view label', async ({ page }) => {
    await registerAndLogin(page);

    // Open create label modal
    await page.getByTestId('create-label-button').click();
    await expect(page.getByTestId('create-label-modal')).toBeVisible();

    // Fill in label details
    await page.getByTestId('label-name-input').fill('My Test Label');
    await page.getByTestId('label-email-input').fill('contact@testlabel.com');
    await page.getByTestId('label-website-input').fill('https://testlabel.com');

    // Submit
    await Promise.all([
      page.waitForURL('/dashboard'),
      page.getByTestId('create-label-submit').click(),
    ]);

    // Should see label in list
    await expect(page.getByTestId('labels-list')).toContainText('My Test Label');

    // Click on label to view details
    await page.getByTestId('label-link').filter({ hasText: 'My Test Label' }).click();

    // Should be on label page
    await expect(page.locator('h1')).toContainText('My Test Label');
  });

  test('can create release with tracks', async ({ page }) => {
    await registerAndLogin(page);

    // Create a label first
    await page.getByTestId('create-label-button').click();
    await page.getByTestId('label-name-input').fill('Release Test Label');
    await Promise.all([
      page.waitForURL('/dashboard'),
      page.getByTestId('create-label-submit').click(),
    ]);

    // Create an artist
    await page.getByTestId('create-artist-button').click();
    await page.getByTestId('artist-name-input').fill('Test Artist');
    await Promise.all([
      page.waitForURL('/dashboard'),
      page.getByTestId('create-artist-submit').click(),
    ]);

    // Go to label page
    await page.getByTestId('label-link').filter({ hasText: 'Release Test Label' }).click();
    await expect(page.locator('h1')).toContainText('Release Test Label');

    // Open create release modal
    await page.getByTestId('create-release-button').click();
    await expect(page.getByTestId('create-release-modal')).toBeVisible();

    // Fill release details
    await page.getByTestId('release-name-input').fill('Test Album');
    await page.getByTestId('release-date-input').fill('2024-06-15');

    // Select release artist (adds hidden input via JS)
    await page.getByTestId('release-artist-select').selectOption({ label: 'Test Artist' });

    // Select format
    await page.getByTestId('format-vinyl').check();

    // Fill track details
    await page.getByTestId('track-name-input').fill('First Track');
    await page.getByTestId('track-duration-input').fill('3:45');

    // Select track artist
    await page.getByTestId('track-artist-select').selectOption({ label: 'Test Artist' });

    // Submit
    await Promise.all([
      page.waitForURL(/\/labels\/\d+$/),
      page.getByTestId('create-release-submit').click(),
    ]);

    // Should see release in list
    await expect(page.getByText('Test Album')).toBeVisible();

    // Click on release
    await page.getByText('Test Album').click();

    // Verify release details
    await expect(page.locator('h1')).toContainText('Test Album');
    await expect(page.getByText('First Track')).toBeVisible();
  });

  test('can create release with track remixers', async ({ page }) => {
    await registerAndLogin(page);

    // Create a label first
    await page.getByTestId('create-label-button').click();
    await page.getByTestId('label-name-input').fill('Remix Label');
    await Promise.all([
      page.waitForURL('/dashboard'),
      page.getByTestId('create-label-submit').click(),
    ]);

    // Create main artist
    await page.getByTestId('create-artist-button').click();
    await page.getByTestId('artist-name-input').fill('Original Artist');
    await Promise.all([
      page.waitForURL('/dashboard'),
      page.getByTestId('create-artist-submit').click(),
    ]);

    // Create remixer
    await page.getByTestId('create-artist-button').click();
    await page.getByTestId('artist-name-input').fill('Cool Remixer');
    await Promise.all([
      page.waitForURL('/dashboard'),
      page.getByTestId('create-artist-submit').click(),
    ]);

    // Go to label page
    await page.getByTestId('label-link').filter({ hasText: 'Remix Label' }).click();
    await expect(page.locator('h1')).toContainText('Remix Label');

    // Open create release modal
    await page.getByTestId('create-release-button').click();
    await expect(page.getByTestId('create-release-modal')).toBeVisible();

    // Fill release details
    await page.getByTestId('release-name-input').fill('Remix EP');
    await page.getByTestId('release-date-input').fill('2024-07-01');

    // Select release artist
    await page.getByTestId('release-artist-select').selectOption({ label: 'Original Artist' });

    // Select format
    await page.getByTestId('format-digital').check();

    // Fill track details
    await page.getByTestId('track-name-input').fill('Dance Track');
    await page.getByTestId('track-duration-input').fill('4:20');

    // Select track artist
    await page.getByTestId('track-artist-select').selectOption({ label: 'Original Artist' });

    // Select track remixer
    await page.getByTestId('track-remixer-select').selectOption({ label: 'Cool Remixer' });

    // Submit
    await Promise.all([
      page.waitForURL(/\/labels\/\d+$/),
      page.getByTestId('create-release-submit').click(),
    ]);

    // Click on release
    await page.getByText('Remix EP').click();

    // Verify release details including remixer
    await expect(page.locator('h1')).toContainText('Remix EP');
    await expect(page.getByText('Dance Track')).toBeVisible();
    await expect(page.getByText(/remixed by/i)).toBeVisible();
    // Check remixer appears as a link (not just in dropdown)
    await expect(page.getByRole('link', { name: 'Cool Remixer' })).toBeVisible();
  });

  test('cannot access other users label', async ({ browser }) => {
    // User 1 creates a label
    const context1 = await browser.newContext();
    const page1 = await context1.newPage();
    const email1 = uniqueEmail();

    await registerAndLogin(page1, email1);
    await page1.getByTestId('create-label-button').click();
    await page1.getByTestId('label-name-input').fill('User One Private Label');
    await Promise.all([
      page1.waitForURL('/dashboard'),
      page1.getByTestId('create-label-submit').click(),
    ]);
    await expect(page1.getByText('User One Private Label')).toBeVisible();

    // User 2 logs in
    const context2 = await browser.newContext();
    const page2 = await context2.newPage();
    const email2 = uniqueEmail();

    await registerAndLogin(page2, email2);

    // User 2's dashboard should NOT show User 1's label
    await expect(page2.getByText('User One Private Label')).not.toBeVisible();

    // Cleanup
    await context1.close();
    await context2.close();
  });

  test('unauthenticated user cannot access protected resources', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login/);

    await page.goto('/labels/1');
    await expect(page).toHaveURL(/\/login/);
  });

  test('invoice extract button appears when PDF uploaded in cost modal', async ({ page }) => {
    await registerAndLogin(page);

    // Create a label and release
    await page.getByTestId('create-label-button').click();
    await page.getByTestId('label-name-input').fill('Cost Test Label');
    await Promise.all([
      page.waitForURL('/dashboard'),
      page.getByTestId('create-label-submit').click(),
    ]);

    // Create an artist
    await page.getByTestId('create-artist-button').click();
    await page.getByTestId('artist-name-input').fill('Cost Test Artist');
    await Promise.all([
      page.waitForURL('/dashboard'),
      page.getByTestId('create-artist-submit').click(),
    ]);

    // Go to label and create release
    await page.getByTestId('label-link').filter({ hasText: 'Cost Test Label' }).click();
    await page.getByTestId('create-release-button').click();
    await page.getByTestId('release-name-input').fill('Cost Test Release');
    await page.getByTestId('release-date-input').fill('2024-01-15');
    await page.getByTestId('release-artist-select').selectOption({ label: 'Cost Test Artist' });
    await page.getByTestId('format-vinyl').check();
    await page.getByTestId('track-name-input').fill('Track 1');
    await page.getByTestId('track-duration-input').fill('3:00');
    await page.getByTestId('track-artist-select').selectOption({ label: 'Cost Test Artist' });
    await Promise.all([
      page.waitForURL(/\/labels\/\d+$/),
      page.getByTestId('create-release-submit').click(),
    ]);

    // Go to release page
    await page.getByText('Cost Test Release').click();
    await expect(page.locator('h1')).toContainText('Cost Test Release');

    // Open Add Cost modal
    await page.locator('[data-bs-target="#addCostModal"]').click();
    await expect(page.locator('#addCostModal')).toBeVisible();

    // Extract button should be hidden initially
    const extractButton = page.getByTestId('extract-invoice-button');
    await expect(extractButton).toBeHidden();

    // Upload a PDF file
    const fileInput = page.getByTestId('cost-document-input');
    await fileInput.setInputFiles({
      name: 'invoice.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from('fake pdf content'),
    });

    // Extract button should now be visible
    await expect(extractButton).toBeVisible();
  });

});
