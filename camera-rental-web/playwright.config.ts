import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: false,
  retries: 0,
  reporter: 'line',
  use: {
    baseURL: 'http://127.0.0.1:4190',
    channel: process.env.PLAYWRIGHT_CHANNEL || 'chrome',
    trace: 'retain-on-failure'
  },
  projects: [
    {
      name: 'mobile-chrome',
      use: {
        ...devices['iPhone 13'],
        browserName: 'chromium',
        channel: process.env.PLAYWRIGHT_CHANNEL || 'chrome'
      }
    }
  ],
  webServer: {
    command: 'bun run dev --host 127.0.0.1 --port 4190',
    url: 'http://127.0.0.1:4190',
    reuseExistingServer: true,
    timeout: 120_000
  }
})
