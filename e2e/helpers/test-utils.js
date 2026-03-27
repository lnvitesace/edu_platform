import { loginUser } from '../seed/api-client.js';
import { TEST_CREDENTIALS } from '../seed/data.js';

export async function loginAsStudent(page) {
  const result = await loginUser(TEST_CREDENTIALS.student);
  await page.goto('/');
  await page.evaluate((authData) => {
    localStorage.setItem('accessToken', authData.accessToken);
    localStorage.setItem('refreshToken', authData.refreshToken);
    localStorage.setItem('user', JSON.stringify(authData.user));
  }, result);
}

export async function loginViaUI(page, username, password) {
  await page.goto('/login');
  await page.fill('#usernameOrEmail', username);
  await page.fill('#password', password);
  await page.click('button[type="submit"]');
}

export async function clearAuth(page) {
  await page.goto('/');
  await page.evaluate(() => localStorage.clear());
}

export async function waitForPageReady(page) {
  await page.waitForLoadState('networkidle');
}
