import { test, expect } from '@playwright/test';

const BACKEND_URL = 'http://localhost:8080';

const uniqueEmail = () => `test-${Date.now()}@example.com`;

async function registerUser(request, email, password = 'password123') {
  const response = await request.post(`${BACKEND_URL}/api/auth/register`, {
    data: { email, password, displayName: 'Test User' },
  });
  expect(response.status()).toBe(201);
}

async function loginViaUI(page, email, password = 'password123') {
  await page.goto('/login');
  await page.getByLabel('Username').fill(email);
  await page.getByLabel('Password').fill(password);
  await Promise.all([
    page.waitForURL('/'),
    page.getByRole('button', { name: 'Log in' }).click(),
  ]);
}

test.describe('Login', () => {

  test('AC-01: successful login redirects to home', async ({ page, request }) => {
    const email = uniqueEmail();
    await registerUser(request, email);

    await page.goto('/login');
    await page.getByLabel('Username').fill(email);
    await page.getByLabel('Password').fill('password123');
    await Promise.all([
      page.waitForURL('/'),
      page.getByRole('button', { name: 'Log in' }).click(),
    ]);

    await expect(page).toHaveURL('/');
  });

  test('AC-02: wrong credentials shows error and stays on login page', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Username').fill('nobody@example.com');
    await page.getByLabel('Password').fill('wrongpassword');
    await page.getByRole('button', { name: 'Log in' }).click();

    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByRole('alert')).toBeVisible();
    // Error must not reveal which field was wrong
    await expect(page.getByRole('alert')).not.toContainText(/username/i);
    await expect(page.getByRole('alert')).not.toContainText(/password/i);
  });

  test('AC-04: logout invalidates session and redirects to login', async ({ page, request }) => {
    const email = uniqueEmail();
    await registerUser(request, email);
    await loginViaUI(page, email);

    await page.getByRole('button', { name: 'Log out' }).click();
    await expect(page).toHaveURL(/\/login/);

    // Confirm session is gone — protected route now redirects back to login
    await page.goto('/');
    await expect(page).toHaveURL(/\/login/);
  });

  test('AC-05: unauthenticated navigation to protected route redirects to login', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/\/login/);
  });

  test('AC-06: navigating to /login while authenticated redirects away', async ({ page, request }) => {
    const email = uniqueEmail();
    await registerUser(request, email);
    await loginViaUI(page, email);

    await page.goto('/login');
    await expect(page).not.toHaveURL(/\/login/);
  });

});
