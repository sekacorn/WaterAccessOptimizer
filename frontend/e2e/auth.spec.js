/**
 * E2E Tests: Authentication Flow
 * Tests user registration, login, and logout
 */

import { test, expect } from '@playwright/test'

test.describe('Authentication', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
  })

  test('should display landing page', async ({ page }) => {
    await expect(page).toHaveTitle(/Water Access Optimizer/i)
    await expect(page.getByRole('link', { name: /login/i })).toBeVisible()
    await expect(page.getByRole('link', { name: /register/i })).toBeVisible()
  })

  test('should navigate to login page', async ({ page }) => {
    await page.getByRole('link', { name: /login/i }).click()
    await expect(page).toHaveURL(/.*login/)
    await expect(page.getByRole('heading', { name: /login/i })).toBeVisible()
  })

  test('should navigate to register page', async ({ page }) => {
    await page.getByRole('link', { name: /register/i }).click()
    await expect(page).toHaveURL(/.*register/)
    await expect(page.getByRole('heading', { name: /register/i })).toBeVisible()
  })

  test('should show validation errors on empty login', async ({ page }) => {
    await page.goto('/login')

    // Click login without filling form
    await page.getByRole('button', { name: /login/i }).click()

    // Should show error
    await expect(page.getByText(/please enter both email and password/i)).toBeVisible()
  })

  test('should show error for invalid email format', async ({ page }) => {
    await page.goto('/login')

    await page.fill('input[type="email"]', 'invalid-email')
    await page.fill('input[type="password"]', 'password123')
    await page.getByRole('button', { name: /login/i }).click()

    await expect(page.getByText(/valid email/i)).toBeVisible()
  })

  test('should complete registration flow', async ({ page }) => {
    await page.goto('/register')

    const timestamp = Date.now()
    await page.fill('input[name="fullName"]', 'Test User')
    await page.fill('input[type="email"]', `test${timestamp}@example.com`)
    await page.fill('input[name="organization"]', 'Test Org')
    await page.fill('input[name="password"]', 'Test123456')
    await page.fill('input[name="confirmPassword"]', 'Test123456')

    await page.getByRole('button', { name: /register/i }).click()

    // Should redirect to dashboard after successful registration
    await expect(page).toHaveURL(/.*dashboard/, { timeout: 10000 })
  })

  test('should show error for password mismatch', async ({ page }) => {
    await page.goto('/register')

    await page.fill('input[name="fullName"]', 'Test User')
    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[name="password"]', 'Test123456')
    await page.fill('input[name="confirmPassword"]', 'Different123')

    await page.getByRole('button', { name: /register/i }).click()

    await expect(page.getByText(/passwords do not match/i)).toBeVisible()
  })

  test('should complete login and logout flow', async ({ page }) => {
    // Login
    await page.goto('/login')
    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[type="password"]', 'Test123456')
    await page.getByRole('button', { name: /login/i }).click()

    // Should be on dashboard
    await expect(page).toHaveURL(/.*dashboard/, { timeout: 10000 })
    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible()

    // Logout
    await page.getByRole('button', { name: /logout/i }).click()

    // Should return to landing page
    await expect(page).toHaveURL('/')
  })

  test('should persist authentication across page reload', async ({ page, context }) => {
    // Login
    await page.goto('/login')
    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[type="password"]', 'Test123456')
    await page.getByRole('button', { name: /login/i }).click()

    await expect(page).toHaveURL(/.*dashboard/)

    // Reload page
    await page.reload()

    // Should still be authenticated
    await expect(page).toHaveURL(/.*dashboard/)
    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible()
  })
})
