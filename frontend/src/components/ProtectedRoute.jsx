import { Navigate } from 'react-router-dom'

/**
 * ProtectedRoute Component
 *
 * Wrapper component for routes that require authentication and specific roles.
 * Redirects to login if not authenticated or to home if insufficient permissions.
 *
 * Usage:
 * <ProtectedRoute requireRole={['ADMIN', 'MODERATOR']}>
 *   <AdminPanel />
 * </ProtectedRoute>
 */
function ProtectedRoute({ children, requireRole = [] }) {
  const token = localStorage.getItem('authToken')
  const userStr = localStorage.getItem('user')

  // Check if user is authenticated
  if (!token || !userStr) {
    return <Navigate to="/login" replace />
  }

  // If no specific role is required, just check authentication
  if (requireRole.length === 0) {
    return children
  }

  // Check if user has required role
  try {
    const user = JSON.parse(userStr)

    // Convert requireRole to array if it's a string
    const requiredRoles = Array.isArray(requireRole) ? requireRole : [requireRole]

    // Check if user's role is in the required roles
    if (requiredRoles.includes(user.role)) {
      return children
    }

    // User doesn't have required role, redirect to home
    console.warn(`Access denied. Required role: ${requiredRoles.join(' or ')}, User role: ${user.role}`)
    return <Navigate to="/" replace />
  } catch (error) {
    // If there's an error parsing user data, redirect to login
    console.error('Error parsing user data:', error)
    localStorage.removeItem('authToken')
    localStorage.removeItem('user')
    return <Navigate to="/login" replace />
  }
}

export default ProtectedRoute
