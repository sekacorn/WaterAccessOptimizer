/**
 * E2E Tests: Risk Assessment Flow
 * Tests assessment creation, execution, and results viewing
 */

import { test, expect } from '@playwright/test'

test.describe('Risk Assessment', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/login')
    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[type="password"]', 'Test123456')
    await page.getByRole('button', { name: /login/i }).click()
    await expect(page).toHaveURL(/.*dashboard/)

    // Navigate to assessment page
    await page.getByRole('link', { name: /assessment/i }).click()
    await expect(page).toHaveURL(/.*assessment/)
  })

  test('should display assessment page', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /risk assessment/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /new assessment/i })).toBeVisible()
  })

  test('should toggle create assessment form', async ({ page }) => {
    // Form should not be visible initially
    await expect(page.getByPlaceholder(/nairobi water access/i)).not.toBeVisible()

    // Click New Assessment button
    await page.getByRole('button', { name: /new assessment/i }).click()

    // Form should be visible
    await expect(page.getByPlaceholder(/nairobi water access/i)).toBeVisible()
    await expect(page.getByPlaceholder(/describe the scope/i)).toBeVisible()
  })

  test('should create new assessment', async ({ page }) => {
    // Open form
    await page.getByRole('button', { name: /new assessment/i }).click()

    const timestamp = Date.now()
    const assessmentName = `Test Assessment ${timestamp}`

    // Fill form
    await page.fill('input[name="name"]', assessmentName)
    await page.fill('textarea[name="description"]', 'E2E test assessment')

    // Submit
    await page.getByRole('button', { name: /create assessment/i }).click()

    // Should redirect to results page or show success
    await page.waitForURL(/.*assessment\/\d+/, { timeout: 10000 })

    // Or should show in list
    // await expect(page.getByText(assessmentName)).toBeVisible({ timeout: 5000 })
  })

  test('should show validation error for empty name', async ({ page }) => {
    await page.getByRole('button', { name: /new assessment/i }).click()

    // Try to submit without name
    await page.getByRole('button', { name: /create assessment/i }).click()

    // Should show error
    await expect(page.getByText(/please enter.*name/i)).toBeVisible({ timeout: 3000 })
  })

  test('should toggle public checkbox', async ({ page }) => {
    await page.getByRole('button', { name: /new assessment/i }).click()

    const checkbox = page.getByRole('checkbox', { name: /make this assessment public/i })

    // Should not be checked by default
    await expect(checkbox).not.toBeChecked()

    // Click to check
    await checkbox.click()
    await expect(checkbox).toBeChecked()

    // Click to uncheck
    await checkbox.click()
    await expect(checkbox).not.toBeChecked()
  })

  test('should display assessment list', async ({ page }) => {
    // Wait for assessments to load
    await page.waitForTimeout(2000)

    // Check if assessments grid or empty state is visible
    const hasAssessments = await page.locator('.assessment-card').count()

    if (hasAssessments > 0) {
      // Should display assessment cards
      await expect(page.locator('.assessment-card').first()).toBeVisible()

      // Should show status
      const statusBadge = page.locator('.status').first()
      await expect(statusBadge).toBeVisible()
    } else {
      // Should show empty state
      await expect(page.getByText(/no assessments yet/i)).toBeVisible()
    }
  })

  test('should display assessment metadata', async ({ page }) => {
    await page.waitForTimeout(2000)

    const assessmentCount = await page.locator('.assessment-card').count()

    if (assessmentCount > 0) {
      const firstCard = page.locator('.assessment-card').first()

      // Check for metadata
      await expect(firstCard.locator('h3')).toBeVisible()
      await expect(firstCard.getByText(/status/i)).toBeVisible()
      await expect(firstCard.getByText(/created/i)).toBeVisible()
    }
  })

  test('should navigate to assessment results', async ({ page }) => {
    await page.waitForTimeout(2000)

    const viewResultsButton = page.getByRole('button', { name: /view results/i }).first()

    if (await viewResultsButton.isVisible()) {
      await viewResultsButton.click()

      // Should navigate to results page
      await expect(page).toHaveURL(/.*assessment\/\d+/, { timeout: 5000 })

      // Should display results heading
      await expect(page.getByRole('heading', { name: /assessment results/i })).toBeVisible()
    }
  })

  test('should run pending assessment', async ({ page }) => {
    await page.waitForTimeout(2000)

    const runButton = page.getByRole('button', { name: /run assessment/i }).first()

    if (await runButton.isVisible()) {
      await runButton.click()

      // Should show notification or redirect
      await page.waitForTimeout(2000)

      // Check for success message or navigation
      const urlChanged = await page.waitForURL(/.*assessment/, { timeout: 5000 })
        .then(() => true)
        .catch(() => false)

      expect(urlChanged).toBeTruthy()
    }
  })

  test('should delete assessment with confirmation', async ({ page }) => {
    await page.waitForTimeout(2000)

    const deleteButton = page.locator('button[title="Delete"]').first()

    if (await deleteButton.isVisible()) {
      // Set up dialog handler before clicking
      page.once('dialog', dialog => {
        expect(dialog.type()).toBe('confirm')
        expect(dialog.message()).toContain('delete')
        dialog.accept()
      })

      await deleteButton.click()

      // Wait for deletion to complete
      await page.waitForTimeout(1000)
    }
  })

  test('should cancel assessment deletion', async ({ page }) => {
    await page.waitForTimeout(2000)

    const deleteButton = page.locator('button[title="Delete"]').first()

    if (await deleteButton.isVisible()) {
      const initialCount = await page.locator('.assessment-card').count()

      // Set up dialog handler to cancel
      page.once('dialog', dialog => {
        dialog.dismiss()
      })

      await deleteButton.click()

      // Wait a bit
      await page.waitForTimeout(500)

      // Count should remain the same
      const newCount = await page.locator('.assessment-card').count()
      expect(newCount).toBe(initialCount)
    }
  })
})
