import { test, expect } from '@playwright/test';
import { loginAsStudent } from '../helpers/test-utils.js';

test.describe('Enrollment', () => {
  test('should enroll in a course and show Continue Learning', async ({ page }) => {
    await loginAsStudent(page);

    // Go to course list and pick a paid course
    await page.goto('/courses');
    await expect(page.locator('.course-card').first()).toBeVisible({ timeout: 15000 });

    // Click on a course card
    await page.locator('.course-card').first().click();
    await expect(page.locator('.course-title')).toBeVisible({ timeout: 15000 });

    const startBtn = page.locator('.start-btn');
    await expect(startBtn).toBeVisible({ timeout: 10000 });

    const btnText = await startBtn.textContent();
    if (btnText === 'Enroll Now') {
      await startBtn.click();
      // Wait for enrollment to complete - button should change
      await expect(startBtn).toHaveText('Continue Learning', { timeout: 15000 });
    } else {
      // Already enrolled
      expect(btnText).toBe('Continue Learning');
    }
  });

  test('should show enrolled course in dashboard', async ({ page }) => {
    await loginAsStudent(page);

    // First ensure enrollment exists (enroll if needed)
    await page.goto('/courses');
    await expect(page.locator('.course-card').first()).toBeVisible({ timeout: 15000 });
    await page.locator('.course-card').first().click();
    await expect(page.locator('.course-title')).toBeVisible({ timeout: 15000 });

    const startBtn = page.locator('.start-btn');
    await expect(startBtn).toBeVisible({ timeout: 10000 });
    const btnText = await startBtn.textContent();
    if (btnText === 'Enroll Now') {
      await startBtn.click();
      await expect(startBtn).toHaveText('Continue Learning', { timeout: 15000 });
    }

    // Go to dashboard
    await page.goto('/dashboard');
    await expect(page.locator('.enrollment-card').first()).toBeVisible({ timeout: 15000 });
  });

  test('should navigate to video player after enrollment', async ({ page }) => {
    await loginAsStudent(page);

    await page.goto('/courses');
    await expect(page.locator('.course-card').first()).toBeVisible({ timeout: 15000 });
    await page.locator('.course-card').first().click();
    await expect(page.locator('.course-title')).toBeVisible({ timeout: 15000 });

    const startBtn = page.locator('.start-btn');
    await expect(startBtn).toBeVisible({ timeout: 10000 });

    // Enroll if needed, then click Continue Learning
    const btnText = await startBtn.textContent();
    if (btnText === 'Enroll Now') {
      await startBtn.click();
      await expect(startBtn).toHaveText('Continue Learning', { timeout: 15000 });
    }
    await startBtn.click();

    // Should navigate to a video player URL
    await expect(page).toHaveURL(/\/courses\/\d+\/lessons\/\d+/, { timeout: 15000 });
  });
});
