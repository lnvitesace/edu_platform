import { test, expect } from '@playwright/test';
import { TEST_CREDENTIALS } from '../seed/data.js';

test.describe('Authentication', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
  });

  test('should register a new user successfully', async ({ page }) => {
    await page.goto('/register');
    const timestamp = Date.now();
    await page.fill('#username', `testuser_${timestamp}`);
    await page.fill('#email', `test_${timestamp}@example.com`);
    await page.fill('#firstName', 'Test');
    await page.fill('#lastName', 'User');
    await page.fill('#password', 'TestPass123');
    await page.fill('#confirmPassword', 'TestPass123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });
  });

  test('should show error when passwords do not match', async ({ page }) => {
    await page.goto('/register');
    await page.fill('#username', 'mismatch_user');
    await page.fill('#email', 'mismatch@example.com');
    await page.fill('#password', 'TestPass123');
    await page.fill('#confirmPassword', 'DifferentPass123');
    await page.click('button[type="submit"]');
    await expect(page.locator('.error-message')).toBeVisible();
  });

  test('should show error for duplicate username', async ({ page }) => {
    await page.goto('/register');
    await page.fill('#username', 'student_zhang');
    await page.fill('#email', 'unique_dup@example.com');
    await page.fill('#password', 'Study123456');
    await page.fill('#confirmPassword', 'Study123456');
    await page.click('button[type="submit"]');
    await expect(page.locator('.error-message')).toBeVisible({ timeout: 10000 });
  });

  test('should login successfully', async ({ page }) => {
    await page.goto('/login');
    await page.fill('#usernameOrEmail', TEST_CREDENTIALS.student.usernameOrEmail);
    await page.fill('#password', TEST_CREDENTIALS.student.password);
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/\/courses/, { timeout: 15000 });
  });

  test('should show error for wrong password', async ({ page }) => {
    await page.goto('/login');
    await page.fill('#usernameOrEmail', TEST_CREDENTIALS.student.usernameOrEmail);
    await page.fill('#password', 'WrongPassword123');
    await page.click('button[type="submit"]');
    await expect(page.locator('.error-message')).toBeVisible({ timeout: 10000 });
  });

  test('should show error for non-existent user', async ({ page }) => {
    await page.goto('/login');
    await page.fill('#usernameOrEmail', 'nonexistent_user_xyz');
    await page.fill('#password', 'SomePassword123');
    await page.click('button[type="submit"]');
    await expect(page.locator('.error-message')).toBeVisible({ timeout: 10000 });
  });

  test('should logout and redirect to login', async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.fill('#usernameOrEmail', TEST_CREDENTIALS.student.usernameOrEmail);
    await page.fill('#password', TEST_CREDENTIALS.student.password);
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/\/courses/, { timeout: 15000 });

    // Click logout in navbar
    await page.click('.navbar-logout-btn');
    await expect(page).toHaveURL(/\/login/, { timeout: 10000 });
  });
});
