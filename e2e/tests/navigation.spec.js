import { test, expect } from '@playwright/test';
import { loginAsStudent } from '../helpers/test-utils.js';

test.describe('Navigation', () => {
  test('should navigate to courses via navbar', async ({ page }) => {
    await page.goto('/');
    // Should redirect to /courses
    await expect(page).toHaveURL(/\/courses/, { timeout: 10000 });
  });

  test('should not show My Learning link when unauthenticated', async ({ page }) => {
    await page.goto('/courses');
    await page.evaluate(() => localStorage.clear());
    await page.reload();
    await expect(page.locator('.course-card').first()).toBeVisible({ timeout: 15000 });
    // My Learning should not be visible
    const myLearning = page.getByRole('link', { name: 'My Learning' });
    await expect(myLearning).not.toBeVisible();
  });

  test('should show My Learning link when authenticated', async ({ page }) => {
    await loginAsStudent(page);
    await page.goto('/courses');
    await expect(page.locator('.course-card').first()).toBeVisible({ timeout: 15000 });
    const myLearning = page.getByRole('link', { name: 'My Learning' });
    await expect(myLearning).toBeVisible();
  });

  test('should redirect to login when accessing dashboard unauthenticated', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login/, { timeout: 10000 });
  });

  test('should redirect to login when accessing video player unauthenticated', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.goto('/courses/1/lessons/1');
    await expect(page).toHaveURL(/\/login/, { timeout: 10000 });
  });
});
