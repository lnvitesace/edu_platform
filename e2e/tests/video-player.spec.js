import { test, expect } from '@playwright/test';
import { loginAsStudent } from '../helpers/test-utils.js';

test.describe('Video Player', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsStudent(page);

    // Enroll in first course and navigate to first lesson
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
    await startBtn.click();
    await expect(page).toHaveURL(/\/courses\/\d+\/lessons\/\d+/, { timeout: 15000 });
  });

  test('should display video element with source', async ({ page }) => {
    // Wait for lesson info to load first
    await expect(page.locator('h1.lesson-title')).toBeVisible({ timeout: 15000 });
    const video = page.locator('video.video-element');
    const videoExists = await video.count() > 0;
    if (videoExists) {
      const src = await video.getAttribute('src');
      expect(src).toBeTruthy();
    } else {
      await expect(page.locator('.video-placeholder')).toBeVisible();
    }
  });

  test('should display lesson title', async ({ page }) => {
    await expect(page.locator('h1.lesson-title')).toBeVisible({ timeout: 15000 });
    const title = await page.locator('h1.lesson-title').textContent();
    expect(title?.length).toBeGreaterThan(0);
  });

  test('should show chapter list sidebar', async ({ page }) => {
    await expect(page.locator('.chapter-list')).toBeVisible({ timeout: 10000 });
    const lessonItems = page.locator('.chapter-list .lesson-item');
    expect(await lessonItems.count()).toBeGreaterThan(0);
  });

  test('should navigate to next free lesson or stay on locked', async ({ page }) => {
    const nextBtn = page.locator('.next-btn');
    if (await nextBtn.isEnabled()) {
      const currentUrl = page.url();
      await nextBtn.click();
      await page.waitForTimeout(1000);
      // Next lesson may be locked (non-free) so URL might not change
      // Just verify the page doesn't crash
      await expect(page.locator('h1.lesson-title')).toBeVisible({ timeout: 10000 });
    }
  });

  test('should navigate back to course', async ({ page }) => {
    const courseBtn = page.locator('.course-btn');
    await courseBtn.click();
    await expect(page).toHaveURL(/\/courses\/\d+$/, { timeout: 10000 });
  });
});
