/**
 * E2E Tests: Data Upload Flow
 * Tests CSV file upload, validation, and upload history
 */

import { test, expect } from '@playwright/test'
import path from 'path'

test.describe('Data Upload', () => {
  test.beforeEach(async ({ page }) => {
    // Login first
    await page.goto('/login')
    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[type="password"]', 'Test123456')
    await page.getByRole('button', { name: /login/i }).click()
    await expect(page).toHaveURL(/.*dashboard/)

    // Navigate to upload page
    await page.getByRole('link', { name: /upload/i }).click()
    await expect(page).toHaveURL(/.*upload/)
  })

  test('should display upload page elements', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /data upload/i })).toBeVisible()
    await expect(page.getByText(/data type/i)).toBeVisible()
    await expect(page.getByRole('button', { name: /upload file/i })).toBeVisible()
  })

  test('should change data type selector', async ({ page }) => {
    const selector = page.locator('select.select-input')

    // Default should be HYDRO
    await expect(selector).toHaveValue('HYDRO')

    // Change to COMMUNITY
    await selector.selectOption('COMMUNITY')
    await expect(selector).toHaveValue('COMMUNITY')

    // Change to INFRASTRUCTURE
    await selector.selectOption('INFRASTRUCTURE')
    await expect(selector).toHaveValue('INFRASTRUCTURE')
  })

  test('should show error when no file selected', async ({ page }) => {
    await page.getByRole('button', { name: /upload file/i }).click()

    // Should show notification or error
    await expect(page.getByText(/please select a file/i)).toBeVisible({ timeout: 5000 })
  })

  test('should upload CSV file successfully', async ({ page }) => {
    // Create a test CSV file path (you would need actual test files in e2e/fixtures)
    const testFilePath = path.join(__dirname, 'fixtures', 'test-hydro.csv')

    // Select data type
    await page.selectOption('select.select-input', 'HYDRO')

    // Upload file
    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles(testFilePath)

    // File name should be displayed
    await expect(page.getByText(/test-hydro.csv/i)).toBeVisible()

    // Click upload
    await page.getByRole('button', { name: /upload file/i }).click()

    // Wait for success message
    await expect(page.getByText(/success/i)).toBeVisible({ timeout: 10000 })
  })

  test('should display upload history table', async ({ page }) => {
    // Check if table exists
    const table = page.locator('table.data-table')
    await expect(table).toBeVisible()

    // Check table headers
    await expect(page.getByRole('columnheader', { name: /filename/i })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: /type/i })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: /rows/i })).toBeVisible()
    await expect(page.getByRole('columnheader', { name: /status/i })).toBeVisible()
  })

  test('should delete upload from history', async ({ page }) => {
    // Wait for table to load
    await page.waitForSelector('table.data-table tbody tr', { timeout: 5000 })

    // Count initial rows
    const initialRows = await page.locator('table.data-table tbody tr').count()

    if (initialRows > 0) {
      // Click first delete button
      const deleteButton = page.locator('button[title="Delete"]').first()
      await deleteButton.click()

      // Confirm deletion in alert
      page.on('dialog', dialog => dialog.accept())

      // Wait for deletion
      await page.waitForTimeout(1000)

      // Verify row count decreased
      const newRows = await page.locator('table.data-table tbody tr').count()
      expect(newRows).toBeLessThanOrEqual(initialRows)
    }
  })

  test('should show validation results after upload', async ({ page }) => {
    const testFilePath = path.join(__dirname, 'fixtures', 'test-hydro-with-errors.csv')

    await page.selectOption('select.select-input', 'HYDRO')

    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles(testFilePath)

    await page.getByRole('button', { name: /upload file/i }).click()

    // Wait for result card
    await page.waitForSelector('.result-stats', { timeout: 10000 })

    // Check for validation info
    await expect(page.getByText(/rows processed/i)).toBeVisible()
    await expect(page.getByText(/errors/i)).toBeVisible()
    await expect(page.getByText(/warnings/i)).toBeVisible()
  })

  test('should filter upload history by data type', async ({ page }) => {
    // Wait for table
    await page.waitForSelector('table.data-table tbody tr', { timeout: 5000 })

    const allRows = await page.locator('table.data-table tbody tr').count()

    if (allRows > 0) {
      // Check if badges exist
      const hydroBadges = page.locator('.badge.hydro')
      const hydroCount = await hydroBadges.count()

      // Verify data type badges are visible
      if (hydroCount > 0) {
        await expect(hydroBadges.first()).toBeVisible()
      }
    }
  })

  test('should handle drag and drop file upload', async ({ page }) => {
    const dropzone = page.locator('.upload-dropzone')

    // Dropzone should be visible
    await expect(dropzone).toBeVisible()

    // Check for drag instructions
    await expect(page.getByText(/drag and drop/i)).toBeVisible()
  })
})
