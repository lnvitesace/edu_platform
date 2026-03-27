import { test, expect } from '@playwright/test';

test.describe('Course Detail', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to course list and click first course
    await page.goto('/courses');
    await expect(page.locator('.course-card').first()).toBeVisible({ timeout: 15000 });
    await page.locator('.course-card').first().click();
    await expect(page.locator('.course-title')).toBeVisible({ timeout: 15000 });
  });

  test('should display course title and meta information', async ({ page }) => {
    await expect(page.locator('.course-title')).toBeVisible();
    await expect(page.locator('.course-price')).toBeVisible();
    await expect(page.locator('.course-meta')).toBeVisible();
  });

  test('should show course description', async ({ page }) => {
    // Description tab should be active by default or click it
    const descTab = page.locator('.tab-btn', { hasText: 'Description' });
    if (await descTab.isVisible()) {
      await descTab.click();
    }
    // Should have some description content
    await expect(page.locator('.tab-content')).toBeVisible();
  });

  test('should show curriculum with chapters and lessons', async ({ page }) => {
    const currTab = page.locator('.tab-btn', { hasText: 'Curriculum' });
    await currTab.click();
    await expect(page.locator('.chapter-item').first()).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.lesson-item').first()).toBeVisible();
  });

  test('should show free badge on free lessons', async ({ page }) => {
    const currTab = page.locator('.tab-btn', { hasText: 'Curriculum' });
    await currTab.click();
    await expect(page.locator('.lesson-item').first()).toBeVisible({ timeout: 10000 });
    // At least one free lesson should exist in seed data
    const freeBadge = page.locator('.lesson-free-badge');
    expect(await freeBadge.count()).toBeGreaterThan(0);
  });

  test('should show "Login to Start" for unauthenticated user', async ({ page }) => {
    await page.evaluate(() => localStorage.clear());
    await page.reload();
    await expect(page.locator('.start-btn')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.start-btn')).toHaveText('Login to Start');
  });
});
