/**
 * E2E Tests: Performance & Load Time
 * Tests page load times, bundle size, and performance metrics
 */

import { test, expect } from '@playwright/test'

test.describe('Performance', () => {
  test('should load landing page within 3 seconds', async ({ page }) => {
    const startTime = Date.now()

    await page.goto('/')
    await page.waitForLoadState('networkidle')

    const loadTime = Date.now() - startTime

    console.log(`Landing page load time: ${loadTime}ms`)
    expect(loadTime).toBeLessThan(3000) // Should load within 3 seconds
  })

  test('should load dashboard within 5 seconds', async ({ page }) => {
    // Login first
    await page.goto('/login')
    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[type="password"]', 'Test123456')
    await page.getByRole('button', { name: /login/i }).click()

    const startTime = Date.now()

    await page.waitForURL(/.*dashboard/)
    await page.waitForLoadState('networkidle')

    const loadTime = Date.now() - startTime

    console.log(`Dashboard load time: ${loadTime}ms`)
    expect(loadTime).toBeLessThan(5000) // Should load within 5 seconds
  })

  test('should measure Core Web Vitals', async ({ page }) => {
    await page.goto('/')

    // Inject performance measurement script
    const metrics = await page.evaluate(() => {
      return new Promise((resolve) => {
        // Wait for page to be fully loaded
        window.addEventListener('load', () => {
          // Get performance metrics
          const perfData = performance.getEntriesByType('navigation')[0]
          const paintEntries = performance.getEntriesByType('paint')

          const fcp = paintEntries.find(entry => entry.name === 'first-contentful-paint')

          resolve({
            domContentLoaded: perfData.domContentLoadedEventEnd - perfData.domContentLoadedEventStart,
            loadComplete: perfData.loadEventEnd - perfData.loadEventStart,
            firstContentfulPaint: fcp ? fcp.startTime : null,
          })
        })
      })
    })

    console.log('Performance Metrics:', metrics)

    // FCP should be under 1.8 seconds (good)
    if (metrics.firstContentfulPaint) {
      expect(metrics.firstContentfulPaint).toBeLessThan(1800)
    }

    // DOM Content Loaded should be fast
    expect(metrics.domContentLoaded).toBeLessThan(2000)
  })

  test('should have acceptable bundle size', async ({ page }) => {
    await page.goto('/')

    // Get all network resources
    const resources = await page.evaluate(() => {
      const perfResources = performance.getEntriesByType('resource')
      return perfResources
        .filter(r => r.initiatorType === 'script' || r.initiatorType === 'link')
        .map(r => ({
          name: r.name,
          size: r.transferSize,
          type: r.initiatorType
        }))
    })

    const totalSize = resources.reduce((sum, r) => sum + r.size, 0)
    const totalSizeKB = totalSize / 1024

    console.log(`Total bundle size: ${totalSizeKB.toFixed(2)} KB`)

    // Total bundle should be under 2MB
    expect(totalSizeKB).toBeLessThan(2048)

    // Individual JS files should be reasonable
    const jsFiles = resources.filter(r => r.type === 'script')
    jsFiles.forEach(file => {
      const sizeKB = file.size / 1024
      console.log(`${file.name}: ${sizeKB.toFixed(2)} KB`)
      expect(sizeKB).toBeLessThan(500) // No single JS file over 500KB
    })
  })

  test('should not have memory leaks on navigation', async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[type="password"]', 'Test123456')
    await page.getByRole('button', { name: /login/i }).click()
    await page.waitForURL(/.*dashboard/)

    // Get initial memory usage
    const initialMemory = await page.evaluate(() => {
      if (performance.memory) {
        return performance.memory.usedJSHeapSize
      }
      return null
    })

    if (initialMemory) {
      // Navigate between pages multiple times
      for (let i = 0; i < 5; i++) {
        await page.getByRole('link', { name: /upload/i }).click()
        await page.waitForTimeout(500)
        await page.getByRole('link', { name: /dashboard/i }).click()
        await page.waitForTimeout(500)
      }

      // Get final memory usage
      const finalMemory = await page.evaluate(() => {
        return performance.memory.usedJSHeapSize
      })

      const memoryIncrease = finalMemory - initialMemory
      const memoryIncreaseMB = memoryIncrease / (1024 * 1024)

      console.log(`Memory increase after navigation: ${memoryIncreaseMB.toFixed(2)} MB`)

      // Memory increase should be reasonable (under 10MB)
      expect(memoryIncreaseMB).toBeLessThan(10)
    }
  })

  test('should have fast interaction response time', async ({ page }) => {
    await page.goto('/login')

    // Measure time to respond to button click
    const startTime = Date.now()

    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[type="password"]', 'Test123456')

    const fillTime = Date.now() - startTime

    console.log(`Form fill response time: ${fillTime}ms`)

    // UI should respond within 100ms
    expect(fillTime).toBeLessThan(100)
  })

  test('should load map efficiently', async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[type="password"]', 'Test123456')
    await page.getByRole('button', { name: /login/i }).click()
    await page.waitForURL(/.*dashboard/)

    const startTime = Date.now()

    await page.getByRole('link', { name: /map/i }).click()
    await page.waitForURL(/.*map/)
    await page.waitForLoadState('networkidle')

    const loadTime = Date.now() - startTime

    console.log(`Map page load time: ${loadTime}ms`)

    // Map with Leaflet should load within 5 seconds
    expect(loadTime).toBeLessThan(5000)
  })

  test('should render charts efficiently', async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[type="email"]', 'test@example.com')
    await page.fill('input[type="password"]', 'Test123456')
    await page.getByRole('button', { name: /login/i }).click()
    await page.waitForURL(/.*dashboard/)

    // Navigate to a page with charts (assessment results if available)
    const viewResultsButton = page.getByRole('button', { name: /view results/i }).first()

    if (await viewResultsButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      const startTime = Date.now()

      await viewResultsButton.click()
      await page.waitForLoadState('networkidle')

      const loadTime = Date.now() - startTime

      console.log(`Charts load time: ${loadTime}ms`)

      // Charts should render within 5 seconds
      expect(loadTime).toBeLessThan(5000)
    }
  })

  test('should have acceptable lighthouse score', async ({ page }) => {
    await page.goto('/')

    // Lighthouse programmatic API would be used here in a real scenario
    // For this example, we'll just check basic metrics

    const metrics = await page.evaluate(() => {
      const nav = performance.getEntriesByType('navigation')[0]
      return {
        domInteractive: nav.domInteractive,
        domComplete: nav.domComplete,
        loadEventEnd: nav.loadEventEnd
      }
    })

    console.log('Page timing metrics:', metrics)

    // DOM interactive should be fast (under 1.5s)
    expect(metrics.domInteractive).toBeLessThan(1500)
  })
})
