# Code Quality & Optimization Guide

**Water Access Optimizer - Frontend Best Practices**

---

## Table of Contents

1. [Performance Optimization](#performance-optimization)
2. [Code Quality Standards](#code-quality-standards)
3. [Bundle Optimization](#bundle-optimization)
4. [React Best Practices](#react-best-practices)
5. [Testing Guidelines](#testing-guidelines)
6. [Accessibility](#accessibility)

---

## Performance Optimization

### Lazy Loading & Code Splitting

**[X]All route components are lazy loaded:**
```javascript
const Dashboard = lazy(() => import('./pages/Dashboard'))
const MapView = lazy(() => import('./pages/MapView'))
```

**Benefits:**
- Reduced initial bundle size
- Faster initial page load
- Better user experience on slow connections

### Memoization

**Use React.memo for expensive components:**
```javascript
import React, { memo } from 'react'

const ExpensiveComponent = memo(function ExpensiveComponent({ data }) {
  // Component logic
})
```

**Use useMemo for expensive calculations:**
```javascript
const filteredData = useMemo(() => {
  return data.filter(item => item.riskLevel === 'HIGH')
}, [data])
```

**Use useCallback for event handlers:**
```javascript
const handleClick = useCallback(() => {
  // Handler logic
}, [dependencies])
```

### Custom Performance Hooks

Located in `src/utils/performance.js`:

1. **useDebounce** - Delay execution until inactivity period
2. **useThrottle** - Limit execution frequency
3. **useVirtualScroll** - Render only visible items
4. **useLazyImage** - Lazy load images
5. **useRenderPerformance** - Measure render time
6. **useWhyDidYouUpdate** - Debug unnecessary re-renders

---

## Code Quality Standards

### ESLint Configuration

**Rules enforced:**
- [X]No console.log (except warn/error)
- [X]No debugger statements
- [X]Prefer const over let
- [X]No var declarations
- [X]Always use === (strict equality)
- [X]Curly braces required
- [X]No duplicate imports
- [X]Proper React Hooks dependencies

### Running Linter

```bash
npm run lint              # Check for issues
npm run lint -- --fix     # Auto-fix issues
```

### Code Style

**Use consistent formatting:**
```javascript
// [X]Good
const user = { name: 'John', email: 'john@example.com' }

// ❌ Bad
const user={name:'John',email:'john@example.com'}
```

**Semicolons:** Not required (ESLint enforced)
**Quotes:** Single quotes preferred
**Max line length:** 120 characters
**Indentation:** 2 spaces

---

## Bundle Optimization

### Current Bundle Strategy

**Vendor chunks:**
- `react-vendor`: React, React-DOM, React Router
- `state`: Zustand
- `http`: Axios
- `map-vendor`: Leaflet, React-Leaflet
- `chart-vendor`: Chart.js, React-Chartjs-2
- `icons`: Lucide React
- `utils`: date-fns, clsx

### Optimization Techniques

**1. Tree Shaking**
- Only import what you need:
```javascript
// [X]Good
import { useState } from 'react'

// ❌ Bad
import * as React from 'react'
```

**2. Dynamic Imports**
```javascript
// Load heavy library only when needed
const loadHeavyLibrary = async () => {
  const lib = await import('heavy-library')
  return lib.default
}
```

**3. Image Optimization**
- Use appropriate formats (WebP, AVIF)
- Compress images before upload
- Use lazy loading for images

**4. Minification**
- Terser minification enabled
- console.log removed in production
- debugger statements removed

### Bundle Analysis

**Check bundle size:**
```bash
npm run build
```

**Expected sizes:**
- Initial bundle: < 300KB (gzipped)
- Vendor chunks: < 500KB each
- Total bundle: < 2MB

---

## React Best Practices

### Component Structure

```javascript
/**
 * Component Name
 * Description of what this component does
 */

import React, { useState, useEffect } from 'react'

function ComponentName({ prop1, prop2 }) {
  // 1. Hooks at the top
  const [state, setState] = useState(null)

  // 2. Effects
  useEffect(() => {
    // Effect logic
  }, [dependencies])

  // 3. Event handlers
  const handleClick = () => {
    // Handler logic
  }

  // 4. Render logic
  if (loading) {
    return <div>Loading...</div>
  }

  // 5. JSX return
  return (
    <div>
      {/* Component JSX */}
    </div>
  )
}

export default ComponentName
```

### State Management

**Use Zustand for global state:**
```javascript
const { user, setUser } = useStore()
```

**Use local state for UI-only state:**
```javascript
const [isOpen, setIsOpen] = useState(false)
```

**Avoid prop drilling:**
```javascript
// ❌ Bad - passing props through many levels
<Parent data={data}>
  <Child data={data}>
    <GrandChild data={data} />
  </Child>
</Parent>

// [X]Good - use context or state management
const data = useStore(state => state.data)
```

### Error Handling

**Always handle errors in async operations:**
```javascript
try {
  const result = await apiCall()
  // Handle success
} catch (error) {
  console.error('Error:', error)
  addNotification({ type: 'error', message: error.message })
}
```

---

## Testing Guidelines

### Unit Tests

**Test state management:**
```javascript
import { renderHook, act } from '@testing-library/react'
import useStore from './useStore'

test('should update user', () => {
  const { result } = renderHook(() => useStore())

  act(() => {
    result.current.setUser({ email: 'test@example.com' })
  })

  expect(result.current.user.email).toBe('test@example.com')
})
```

### Component Tests

**Test user interactions:**
```javascript
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

test('should handle button click', async () => {
  const user = userEvent.setup()
  render(<Button onClick={handleClick} />)

  await user.click(screen.getByRole('button'))

  expect(handleClick).toHaveBeenCalled()
})
```

### E2E Tests

**Test critical user flows:**
```javascript
test('should complete login flow', async ({ page }) => {
  await page.goto('/login')
  await page.fill('input[type="email"]', 'test@example.com')
  await page.fill('input[type="password"]', 'password')
  await page.click('button[type="submit"]')

  await expect(page).toHaveURL('/dashboard')
})
```

### Coverage Goals

- **Unit tests:** > 80%
- **Integration tests:** > 70%
- **E2E tests:** Critical paths covered

---

## Accessibility

### ARIA Labels

**Use semantic HTML:**
```javascript
// [X]Good
<button onClick={handleClick}>Submit</button>

// ❌ Bad
<div onClick={handleClick}>Submit</div>
```

**Add ARIA attributes when needed:**
```javascript
<button
  aria-label="Close dialog"
  aria-expanded={isOpen}
>
  ✕
</button>
```

### Keyboard Navigation

**Ensure all interactive elements are keyboard accessible:**
```javascript
<div
  role="button"
  tabIndex={0}
  onKeyDown={(e) => e.key === 'Enter' && handleClick()}
  onClick={handleClick}
>
  Click me
</div>
```

### Color Contrast

**Maintain WCAG AA standards:**
- Normal text: 4.5:1 contrast ratio
- Large text: 3:1 contrast ratio
- Interactive elements: Clearly distinguishable

### Focus Indicators

**Always show focus outlines:**
```css
button:focus {
  outline: 2px solid var(--primary);
  outline-offset: 2px;
}
```

---

## Performance Monitoring

### Metrics to Track

1. **First Contentful Paint (FCP)** - < 1.8s
2. **Largest Contentful Paint (LCP)** - < 2.5s
3. **Time to Interactive (TTI)** - < 3.8s
4. **Total Blocking Time (TBT)** - < 200ms
5. **Cumulative Layout Shift (CLS)** - < 0.1

### Monitoring in Development

```javascript
// In App.jsx or index.jsx
if (process.env.NODE_ENV === 'development') {
  import('./utils/performance').then(({ logBundleInfo }) => {
    logBundleInfo()
  })
}
```

### Production Monitoring

Consider integrating:
- Google Analytics
- Sentry for error tracking
- Web Vitals reporting
- Custom performance metrics

---

## Pre-commit Checklist

Before committing code:

- [ ] Run linter: `npm run lint`
- [ ] Run tests: `npm test`
- [ ] Check bundle size: `npm run build`
- [ ] Test in browser
- [ ] Check console for errors/warnings
- [ ] Verify accessibility with screen reader
- [ ] Test on mobile viewport

---

## Resources

- [React Performance Optimization](https://react.dev/learn/render-and-commit)
- [Vite Build Optimization](https://vitejs.dev/guide/build.html)
- [Web Vitals](https://web.dev/vitals/)
- [WCAG Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [ESLint Rules](https://eslint.org/docs/rules/)

---

**Last Updated:** 2026-02-04
