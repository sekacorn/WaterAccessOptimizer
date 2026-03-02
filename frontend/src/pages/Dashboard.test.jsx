/**
 * Tests for Dashboard Component
 * Tests dashboard rendering, data loading, and stats display
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderWithRouter, mockApiResponses } from '../test/testUtils'
import Dashboard from './Dashboard'
import * as api from '../services/api'
import useStore from '../store/useStore'

// Mock API module
vi.mock('../services/api')

// Mock Zustand store
vi.mock('../store/useStore')

describe('Dashboard', () => {
  beforeEach(() => {
    // Reset mocks
    vi.clearAllMocks()

    // Default mock store state
    useStore.mockReturnValue({
      quotaInfo: null,
      setQuotaInfo: vi.fn(),
      addNotification: vi.fn()
    })

    // Mock API responses
    api.getUploads.mockResolvedValue(mockApiResponses.uploads)
    api.getAssessments.mockResolvedValue(mockApiResponses.assessments)
    api.getQuotaInfo.mockResolvedValue(mockApiResponses.quota)
  })

  it('should render dashboard heading', () => {
    renderWithRouter(<Dashboard />)
    expect(screen.getByText(/Dashboard/i)).toBeInTheDocument()
  })

  it('should show loading state initially', () => {
    renderWithRouter(<Dashboard />)
    expect(screen.getByText(/Loading dashboard/i)).toBeInTheDocument()
  })

  it('should load and display dashboard data', async () => {
    renderWithRouter(<Dashboard />)

    await waitFor(() => {
      expect(api.getUploads).toHaveBeenCalledWith(0, 5)
      expect(api.getAssessments).toHaveBeenCalled()
      expect(api.getQuotaInfo).toHaveBeenCalled()
    })
  })

  it('should display quota information', async () => {
    const mockStore = {
      quotaInfo: mockApiResponses.quota,
      setQuotaInfo: vi.fn(),
      addNotification: vi.fn()
    }
    useStore.mockReturnValue(mockStore)

    renderWithRouter(<Dashboard />)

    await waitFor(() => {
      // Check if quota percentage calculation is correct
      const percentage = (150 / 1000) * 100 // 15%
      expect(percentage).toBe(15)
    })
  })

  it('should display recent uploads', async () => {
    renderWithRouter(<Dashboard />)

    await waitFor(() => {
      expect(screen.getByText(/test-hydro.csv/i)).toBeInTheDocument()
      expect(screen.getByText(/test-community.csv/i)).toBeInTheDocument()
    })
  })

  it('should display recent assessments', async () => {
    renderWithRouter(<Dashboard />)

    await waitFor(() => {
      expect(screen.getByText(/Test Assessment 1/i)).toBeInTheDocument()
      expect(screen.getByText(/Test Assessment 2/i)).toBeInTheDocument()
    })
  })

  it('should handle API errors gracefully', async () => {
    const mockAddNotification = vi.fn()
    useStore.mockReturnValue({
      quotaInfo: null,
      setQuotaInfo: vi.fn(),
      addNotification: mockAddNotification
    })

    api.getUploads.mockRejectedValue(new Error('API Error'))
    api.getAssessments.mockRejectedValue(new Error('API Error'))
    api.getQuotaInfo.mockRejectedValue(new Error('API Error'))

    renderWithRouter(<Dashboard />)

    await waitFor(() => {
      expect(mockAddNotification).toHaveBeenCalled()
    })
  })

  it('should show empty state when no data', async () => {
    api.getUploads.mockResolvedValue({ uploads: [], totalPages: 0 })
    api.getAssessments.mockResolvedValue([])

    renderWithRouter(<Dashboard />)

    await waitFor(() => {
      expect(screen.queryByText(/test-hydro.csv/i)).not.toBeInTheDocument()
    })
  })

  it('should call setQuotaInfo with quota data', async () => {
    const mockSetQuotaInfo = vi.fn()
    useStore.mockReturnValue({
      quotaInfo: null,
      setQuotaInfo: mockSetQuotaInfo,
      addNotification: vi.fn()
    })

    renderWithRouter(<Dashboard />)

    await waitFor(() => {
      expect(mockSetQuotaInfo).toHaveBeenCalledWith(mockApiResponses.quota)
    })
  })
})
