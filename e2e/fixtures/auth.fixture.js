import { test as base } from '@playwright/test';
import { loginUser } from '../seed/api-client.js';
import { TEST_CREDENTIALS } from '../seed/data.js';

// Fixture that provides an already-authenticated page (student_zhang)
export const test = base.extend({
  authenticatedPage: async ({ page }, use) => {
    const result = await loginUser(TEST_CREDENTIALS.student);
    await page.goto('/');
    await page.evaluate((authData) => {
      localStorage.setItem('accessToken', authData.accessToken);
      localStorage.setItem('refreshToken', authData.refreshToken);
      localStorage.setItem('user', JSON.stringify(authData.user));
    }, result);
    await page.reload();
    await use(page);
  },
});

export { expect } from '@playwright/test';
