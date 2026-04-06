import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env.E2E_TARGET_URL || 'http://localhost:5173';
const isLocalhost = baseURL.includes('localhost');

export default defineConfig({
  testDir: './',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
  ],
  // Only start local servers when not testing against external URL
  ...(isLocalhost && {
    webServer: [
      {
        command: './gradlew bootRun',
        cwd: '../backend',
        url: 'http://localhost:8080',
        reuseExistingServer: !process.env.CI,
        timeout: 120000,
      },
      {
        command: 'npm run dev',
        cwd: '../frontend',
        url: 'http://localhost:5173',
        reuseExistingServer: !process.env.CI,
        timeout: 30000,
      },
    ],
  }),
});
