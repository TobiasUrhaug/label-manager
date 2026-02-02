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

});
