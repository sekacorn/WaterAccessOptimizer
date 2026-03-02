/**
 * Performance Optimization Utilities
 * Helpers for React performance optimization
 */

import { useMemo, useCallback, useRef, useEffect } from 'react'

/**
 * Debounce hook for performance optimization
 * Delays execution until after wait period of inactivity
 *
 * @param {Function} callback - Function to debounce
 * @param {number} delay - Delay in milliseconds
 * @returns {Function} Debounced function
 */
export function useDebounce(callback, delay = 300) {
  const timeoutRef = useRef(null)

  const debouncedCallback = useCallback(
    (...args) => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current)
      }

      timeoutRef.current = setTimeout(() => {
        callback(...args)
      }, delay)
    },
    [callback, delay]
  )

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current)
      }
    }
  }, [])

  return debouncedCallback
}

/**
 * Throttle hook for performance optimization
 * Ensures function is called at most once per specified time period
 *
 * @param {Function} callback - Function to throttle
 * @param {number} delay - Minimum delay between calls in milliseconds
 * @returns {Function} Throttled function
 */
export function useThrottle(callback, delay = 300) {
  const lastRun = useRef(Date.now())

  const throttledCallback = useCallback(
    (...args) => {
      const now = Date.now()
      if (now - lastRun.current >= delay) {
        callback(...args)
        lastRun.current = now
      }
    },
    [callback, delay]
  )

  return throttledCallback
}

/**
 * Memoize expensive calculations
 * Re-computes only when dependencies change
 *
 * @param {Function} factory - Function that returns computed value
 * @param {Array} deps - Dependency array
 * @returns {*} Memoized value
 */
export function useMemoizedValue(factory, deps) {
  return useMemo(factory, deps)
}

/**
 * Memoize callback functions
 * Prevents unnecessary re-renders of child components
 *
 * @param {Function} callback - Callback function
 * @param {Array} deps - Dependency array
 * @returns {Function} Memoized callback
 */
export function useMemoizedCallback(callback, deps) {
  return useCallback(callback, deps)
}

/**
 * Virtual scrolling helper - calculate visible items
 * For large lists, only render visible items
 *
 * @param {Array} items - All items
 * @param {number} itemHeight - Height of each item
 * @param {number} containerHeight - Height of container
 * @param {number} scrollTop - Current scroll position
 * @returns {Object} Visible items and offset
 */
export function useVirtualScroll(items, itemHeight, containerHeight, scrollTop) {
  return useMemo(() => {
    const startIndex = Math.floor(scrollTop / itemHeight)
    const endIndex = Math.min(
      items.length - 1,
      Math.ceil((scrollTop + containerHeight) / itemHeight)
    )

    const visibleItems = items.slice(startIndex, endIndex + 1)
    const offsetY = startIndex * itemHeight

    return {
      visibleItems,
      offsetY,
      startIndex,
      endIndex,
    }
  }, [items, itemHeight, containerHeight, scrollTop])
}

/**
 * Measure component render performance
 * Logs render time in development mode
 *
 * @param {string} componentName - Name of component
 */
export function useRenderPerformance(componentName) {
  const renderCount = useRef(0)

  useEffect(() => {
    renderCount.current += 1

    if (process.env.NODE_ENV === 'development') {
      const startTime = performance.now()

      return () => {
        const endTime = performance.now()
        const renderTime = endTime - startTime

        if (renderTime > 16) {
          // Warn if render takes more than one frame (16ms)
          console.warn(
            `[Performance] ${componentName} render #${renderCount.current} took ${renderTime.toFixed(2)}ms`
          )
        }
      }
    }
  })
}

/**
 * Check if values have changed (for optimization debugging)
 * Useful for identifying unnecessary re-renders
 *
 * @param {Object} values - Object of values to track
 * @param {string} componentName - Name of component
 */
export function useWhyDidYouUpdate(values, componentName) {
  const previousValues = useRef(values)

  useEffect(() => {
    if (process.env.NODE_ENV === 'development') {
      const changes = {}
      let hasChanges = false

      Object.keys(values).forEach((key) => {
        if (previousValues.current[key] !== values[key]) {
          changes[key] = {
            from: previousValues.current[key],
            to: values[key],
          }
          hasChanges = true
        }
      })

      if (hasChanges) {
        console.log(`[WhyDidYouUpdate] ${componentName}:`, changes)
      }

      previousValues.current = values
    }
  })
}

/**
 * Lazy load images for performance
 * Returns a ref to attach to img element
 *
 * @returns {Object} Ref object
 */
export function useLazyImage() {
  const imgRef = useRef(null)

  useEffect(() => {
    if (!imgRef.current) {
      return
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const img = entry.target
            const src = img.getAttribute('data-src')
            if (src) {
              img.src = src
              img.removeAttribute('data-src')
            }
            observer.unobserve(img)
          }
        })
      },
      { rootMargin: '50px' }
    )

    observer.observe(imgRef.current)

    return () => {
      if (imgRef.current) {
        observer.unobserve(imgRef.current)
      }
    }
  }, [])

  return imgRef
}

/**
 * Batch state updates for performance
 * Groups multiple state updates into single re-render
 *
 * @param {Function} callback - Function containing state updates
 */
export function batchUpdates(callback) {
  // React 18 automatically batches updates
  // This is a no-op for compatibility
  callback()
}

/**
 * Calculate and log bundle size information
 * Development helper for bundle optimization
 */
export function logBundleInfo() {
  if (process.env.NODE_ENV === 'development') {
    const resources = performance.getEntriesByType('resource')
    const jsResources = resources.filter((r) => r.name.endsWith('.js'))
    const cssResources = resources.filter((r) => r.name.endsWith('.css'))

    const totalJsSize = jsResources.reduce((sum, r) => sum + r.transferSize, 0)
    const totalCssSize = cssResources.reduce((sum, r) => sum + r.transferSize, 0)

    console.group('📦 Bundle Information')
    console.log(`JS files: ${jsResources.length}`)
    console.log(`Total JS size: ${(totalJsSize / 1024).toFixed(2)} KB`)
    console.log(`CSS files: ${cssResources.length}`)
    console.log(`Total CSS size: ${(totalCssSize / 1024).toFixed(2)} KB`)
    console.log(`Total bundle: ${((totalJsSize + totalCssSize) / 1024).toFixed(2)} KB`)
    console.groupEnd()
  }
}
