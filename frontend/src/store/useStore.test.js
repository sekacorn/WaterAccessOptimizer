/**
 * Tests for Zustand Store
 * Tests state management, actions, and persistence
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import useStore from './useStore'

describe('useStore', () => {
  beforeEach(() => {
    // Clear localStorage before each test
    localStorage.clear()
    // Reset store state
    const { result } = renderHook(() => useStore())
    act(() => {
      result.current.logout()
      result.current.setAssessments([])
      result.current.setUploads([])
    })
  })

  describe('Authentication', () => {
    it('should initialize with unauthenticated state', () => {
      const { result } = renderHook(() => useStore())

      expect(result.current.isAuthenticated).toBe(false)
      expect(result.current.user).toBe(null)
      expect(result.current.token).toBe(null)
    })

    it('should set user and authenticate', () => {
      const { result } = renderHook(() => useStore())
      const mockUser = {
        email: 'test@example.com',
        fullName: 'Test User',
        role: 'USER'
      }

      act(() => {
        result.current.setUser(mockUser)
      })

      expect(result.current.isAuthenticated).toBe(true)
      expect(result.current.user).toEqual(mockUser)
    })

    it('should set token and authenticate', () => {
      const { result } = renderHook(() => useStore())
      const mockToken = 'mock-jwt-token'

      act(() => {
        result.current.setToken(mockToken)
      })

      expect(result.current.isAuthenticated).toBe(true)
      expect(result.current.token).toBe(mockToken)
      expect(localStorage.setItem).toHaveBeenCalledWith('authToken', mockToken)
    })

    it('should logout and clear state', () => {
      const { result } = renderHook(() => useStore())

      // Set up authenticated state
      act(() => {
        result.current.setUser({ email: 'test@example.com' })
        result.current.setToken('token')
      })

      // Logout
      act(() => {
        result.current.logout()
      })

      expect(result.current.isAuthenticated).toBe(false)
      expect(result.current.user).toBe(null)
      expect(result.current.token).toBe(null)
      expect(localStorage.removeItem).toHaveBeenCalledWith('authToken')
    })
  })

  describe('UI State', () => {
    it('should toggle sidebar', () => {
      const { result } = renderHook(() => useStore())
      const initialState = result.current.sidebarOpen

      act(() => {
        result.current.toggleSidebar()
      })

      expect(result.current.sidebarOpen).toBe(!initialState)

      act(() => {
        result.current.toggleSidebar()
      })

      expect(result.current.sidebarOpen).toBe(initialState)
    })

    it('should have default map center and zoom', () => {
      const { result } = renderHook(() => useStore())

      expect(result.current.mapCenter).toEqual([36.8219, -1.2921]) // Nairobi
      expect(result.current.mapZoom).toBe(10)
    })
  })

  describe('Data State', () => {
    it('should set uploads', () => {
      const { result } = renderHook(() => useStore())
      const mockUploads = [
        { id: 1, filename: 'test1.csv', dataType: 'HYDRO' },
        { id: 2, filename: 'test2.csv', dataType: 'COMMUNITY' }
      ]

      act(() => {
        result.current.setUploads(mockUploads)
      })

      expect(result.current.uploads).toEqual(mockUploads)
      expect(result.current.uploads.length).toBe(2)
    })

    it('should add upload to beginning of list', () => {
      const { result } = renderHook(() => useStore())
      const existingUpload = { id: 1, filename: 'existing.csv' }
      const newUpload = { id: 2, filename: 'new.csv' }

      act(() => {
        result.current.setUploads([existingUpload])
      })

      act(() => {
        result.current.addUpload(newUpload)
      })

      expect(result.current.uploads[0]).toEqual(newUpload)
      expect(result.current.uploads[1]).toEqual(existingUpload)
      expect(result.current.uploads.length).toBe(2)
    })

    it('should set assessments', () => {
      const { result } = renderHook(() => useStore())
      const mockAssessments = [
        { id: 1, name: 'Assessment 1', status: 'COMPLETED' },
        { id: 2, name: 'Assessment 2', status: 'PENDING' }
      ]

      act(() => {
        result.current.setAssessments(mockAssessments)
      })

      expect(result.current.assessments).toEqual(mockAssessments)
      expect(result.current.assessments.length).toBe(2)
    })

    it('should set current assessment', () => {
      const { result } = renderHook(() => useStore())
      const mockAssessment = {
        id: 1,
        name: 'Test Assessment',
        summary: { totalRecords: 100 }
      }

      act(() => {
        result.current.setCurrentAssessment(mockAssessment)
      })

      expect(result.current.currentAssessment).toEqual(mockAssessment)
    })

    it('should set quota info', () => {
      const { result } = renderHook(() => useStore())
      const mockQuota = {
        storageUsedMb: 50,
        storageQuotaMb: 1000,
        uploadsUsed: 10,
        uploadsQuota: 100
      }

      act(() => {
        result.current.setQuotaInfo(mockQuota)
      })

      expect(result.current.quotaInfo).toEqual(mockQuota)
    })
  })

  describe('Notifications', () => {
    beforeEach(() => {
      vi.useFakeTimers()
    })

    afterEach(() => {
      vi.useRealTimers()
    })

    it('should add notification', () => {
      const { result } = renderHook(() => useStore())
      const notification = {
        type: 'success',
        message: 'Test notification'
      }

      act(() => {
        result.current.addNotification(notification)
      })

      expect(result.current.notifications.length).toBe(1)
      expect(result.current.notifications[0].message).toBe(notification.message)
      expect(result.current.notifications[0].type).toBe(notification.type)
      expect(result.current.notifications[0].id).toBeDefined()
    })

    it('should auto-remove notification after 5 seconds', () => {
      const { result } = renderHook(() => useStore())
      const notification = {
        type: 'info',
        message: 'Auto-remove test'
      }

      act(() => {
        result.current.addNotification(notification)
      })

      expect(result.current.notifications.length).toBe(1)

      // Fast-forward time by 5 seconds
      act(() => {
        vi.advanceTimersByTime(5000)
      })

      expect(result.current.notifications.length).toBe(0)
    })

    it('should manually remove notification', () => {
      const { result } = renderHook(() => useStore())

      act(() => {
        result.current.addNotification({ type: 'error', message: 'Error 1' })
        result.current.addNotification({ type: 'error', message: 'Error 2' })
      })

      expect(result.current.notifications.length).toBe(2)
      const firstNotificationId = result.current.notifications[0].id

      act(() => {
        result.current.removeNotification(firstNotificationId)
      })

      expect(result.current.notifications.length).toBe(1)
      expect(result.current.notifications[0].message).toBe('Error 2')
    })
  })

  describe('Persistence', () => {
    it('should persist authentication state', () => {
      const { result } = renderHook(() => useStore())
      const user = { email: 'persist@test.com', fullName: 'Persist User' }
      const token = 'persist-token'

      act(() => {
        result.current.setUser(user)
        result.current.setToken(token)
      })

      // Verify localStorage was called
      expect(localStorage.setItem).toHaveBeenCalled()
    })

    it('should persist UI state', () => {
      const { result } = renderHook(() => useStore())

      act(() => {
        result.current.toggleSidebar()
      })

      // Verify localStorage was called for persisting state
      expect(localStorage.setItem).toHaveBeenCalled()
    })
  })
})
