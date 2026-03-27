import { test, expect } from '@playwright/test';
import { loginAsStudent } from '../helpers/test-utils.js';

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsStudent(page);
    await page.goto('/dashboard');
    await expect(page.getByText(/Welcome/)).toBeVisible({ timeout: 15000 });
  });

  test('should display welcome message', async ({ page }) => {
    await expect(page.getByText(/Welcome/)).toBeVisible({ timeout: 10000 });
  });

  test('should display profile information', async ({ page }) => {
    await expect(page.locator('.profile-section')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.profile-item').first()).toBeVisible();
  });

  test('should edit profile and save changes', async ({ page }) => {
    // Click Edit Profile
    const editBtn = page.locator('button', { hasText: 'Edit Profile' });
    await expect(editBtn).toBeVisible({ timeout: 10000 });
    await editBtn.click();

    // Should now show edit form
    const firstNameInput = page.locator('input[name="firstName"]');
    await expect(firstNameInput).toBeVisible();

    // Update first name
    await firstNameInput.fill('UpdatedName');

    // Save
    const saveBtn = page.locator('button', { hasText: 'Save Changes' });
    await saveBtn.click();

    // Should return to view mode
    await expect(editBtn).toBeVisible({ timeout: 10000 });
  });

  test('should cancel editing profile', async ({ page }) => {
    const editBtn = page.locator('button', { hasText: 'Edit Profile' });
    await expect(editBtn).toBeVisible({ timeout: 10000 });
    await editBtn.click();

    const cancelBtn = page.locator('button', { hasText: 'Cancel' });
    await cancelBtn.click();

    // Should return to view mode
    await expect(editBtn).toBeVisible({ timeout: 5000 });
  });

  test('should show My Courses section', async ({ page }) => {
    const myCoursesSection = page.getByText(/My Courses/);
    await expect(myCoursesSection).toBeVisible({ timeout: 10000 });
  });
});
