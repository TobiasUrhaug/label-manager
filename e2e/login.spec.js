import { test, expect } from '@playwright/test';

const DEFAULT_PASSWORD = 'password123';

function apiBaseUrl(baseURL) {
  const url = new URL(baseURL);
  url.port = '8080';
  return url.origin;
}

const uniqueEmail = () => `test-${crypto.randomUUID()}@example.com`;

async function registerUser(request, baseURL, email, password = DEFAULT_PASSWORD) {
  const response = await request.post(`${apiBaseUrl(baseURL)}/api/auth/register`, {
    data: { email, password, displayName: 'Test User' },
  });
  expect(response.status()).toBe(201);
}

async function loginViaUI(page, email, password = DEFAULT_PASSWORD) {
  await page.goto('/login');
  await page.getByTestId('login-email').fill(email);
  await page.getByTestId('login-password').fill(password);
  await Promise.all([
    page.waitForURL('/'),
    page.getByTestId('login-submit').click(),
  ]);
}

test.describe('Login', () => {

  test('AC-01: successful login redirects to home', async ({ page, request, baseURL }) => {
    const email = uniqueEmail();
    await registerUser(request, baseURL, email);

    await page.goto('/login');
    await page.getByTestId('login-email').fill(email);
    await page.getByTestId('login-password').fill(DEFAULT_PASSWORD);
    await Promise.all([
      page.waitForURL('/'),
      page.getByTestId('login-submit').click(),
    ]);

    await expect(page).toHaveURL('/');
  });

  test('AC-02: wrong credentials shows error and stays on login page', async ({ page }) => {
    await page.goto('/login');
    await page.getByTestId('login-email').fill('nobody@example.com');
    await page.getByTestId('login-password').fill('wrongpassword');
    await page.getByTestId('login-submit').click();

    await expect(page).toHaveURL(/\/login/);
    const error = page.getByTestId('login-error');
    await expect(error).toBeVisible();
    // Error must not reveal which field was wrong
    await expect(error).not.toContainText(/username/i);
    await expect(error).not.toContainText(/password/i);
  });

  test('AC-04: logout invalidates session and redirects to login', async ({ page, request, baseURL }) => {
    const email = uniqueEmail();
    await registerUser(request, baseURL, email);
    await loginViaUI(page, email);

    await page.getByTestId('logout-button').click();
    await expect(page).toHaveURL(/\/login/);

    // Confirm session is gone — protected route now redirects back to login
    await page.goto('/');
    await expect(page).toHaveURL(/\/login/);
  });

  test('AC-05: unauthenticated navigation to protected route redirects to login', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/\/login/);
  });

  test('AC-06: navigating to /login while authenticated redirects away', async ({ page, request, baseURL }) => {
    const email = uniqueEmail();
    await registerUser(request, baseURL, email);
    await loginViaUI(page, email);

    await page.goto('/login');
    await expect(page).not.toHaveURL(/\/login/);
  });

});
