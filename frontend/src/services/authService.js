/**
 * Authentication Service
 *
 * Handles all authentication-related API calls and token management
 * Integrates with auth-service backend (port 8081)
 */

const API_BASE_URL = import.meta.env.VITE_AUTH_API_URL || 'http://localhost:8081/api/v1/auth'

class AuthService {
  /**
   * Register a new user
   */
  async register(email, password, fullName, organization = null) {
    const response = await fetch(`${API_BASE_URL}/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password, fullName, organization })
    })

    const data = await response.json()

    if (!response.ok) {
      throw new Error(data.message || 'Registration failed')
    }

    if (data.token) {
      this.setAuth(data.token, data)
    }

    return data
  }

  /**
   * Login user
   */
  async login(email, password) {
    const response = await fetch(`${API_BASE_URL}/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password })
    })

    const data = await response.json()

    if (!response.ok) {
      throw new Error(data.message || 'Login failed')
    }

    this.setAuth(data.token, data)

    return data
  }

  /**
   * Verify MFA code
   */
  async verifyMfa(tempToken, code, trustDevice = false) {
    const response = await fetch(`${API_BASE_URL}/mfa/verify`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ tempToken, code, trustDevice })
    })

    const data = await response.json()

    if (!response.ok) {
      throw new Error(data.error || 'MFA verification failed')
    }

    this.setAuth(data.token, data.user)

    return data
  }

  /**
   * Check if SSO is enabled for email domain
   */
  async checkSsoDomain(email) {
    try {
      const response = await fetch(`${API_BASE_URL}/sso/check-domain?email=${encodeURIComponent(email)}`)

      if (!response.ok) {
        return { ssoEnabled: false }
      }

      return await response.json()
    } catch (error) {
      return { ssoEnabled: false }
    }
  }

  /**
   * Initiate SSO login
   */
  async initiateSsoLogin(provider, email) {
    const response = await fetch(`${API_BASE_URL}/sso/login/${provider}?email=${encodeURIComponent(email)}`)

    if (!response.ok) {
      throw new Error('SSO login failed')
    }

    return response.json()
  }

  /**
   * Logout user
   */
  logout() {
    localStorage.removeItem('authToken')
    localStorage.removeItem('user')
  }

  /**
   * Get current user from localStorage
   */
  getCurrentUser() {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      try {
        return JSON.parse(userStr)
      } catch {
        return null
      }
    }
    return null
  }

  /**
   * Get auth token
   */
  getToken() {
    return localStorage.getItem('authToken')
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated() {
    return !!this.getToken()
  }

  /**
   * Check if user has specific role
   */
  hasRole(role) {
    const user = this.getCurrentUser()
    if (!user) {
      return false
    }

    if (Array.isArray(role)) {
      return role.includes(user.role)
    }

    return user.role === role
  }

  /**
   * Store auth token and user data
   */
  setAuth(token, user) {
    localStorage.setItem('authToken', token)
    localStorage.setItem('user', JSON.stringify(user))
  }

  /**
   * Fetch current user from API (validate token)
   */
  async fetchCurrentUser() {
    try {
      const token = this.getToken()
      if (!token) {
        throw new Error('No token found')
      }

      const response = await fetch(`${API_BASE_URL}/me`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      if (!response.ok) {
        throw new Error('Token validation failed')
      }

      const user = await response.json()
      localStorage.setItem('user', JSON.stringify(user))

      return user
    } catch (error) {
      this.logout()
      throw error
    }
  }

  /**
   * Change password
   */
  async changePassword(currentPassword, newPassword) {
    const token = this.getToken()
    if (!token) {
      throw new Error('Not authenticated')
    }

    const response = await fetch(`${API_BASE_URL}/change-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ currentPassword, newPassword })
    })

    const data = await response.json()

    if (!response.ok) {
      throw new Error(data.error || 'Password change failed')
    }

    return data
  }

  /**
   * Request password reset
   */
  async requestPasswordReset(email) {
    const response = await fetch(`${API_BASE_URL}/forgot-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email })
    })

    const data = await response.json()

    if (!response.ok) {
      throw new Error(data.error || 'Password reset request failed')
    }

    return data
  }

  /**
   * Reset password with token
   */
  async resetPassword(token, newPassword) {
    const response = await fetch(`${API_BASE_URL}/reset-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ token, newPassword })
    })

    const data = await response.json()

    if (!response.ok) {
      throw new Error(data.error || 'Password reset failed')
    }

    return data
  }
}

export default new AuthService()
