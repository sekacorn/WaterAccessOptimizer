/**
 * Tests for RiskAssessment Component
 * Tests assessment creation, listing, and management
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithRouter, mockApiResponses } from '../test/testUtils'
import RiskAssessment from './RiskAssessment'
import * as api from '../services/api'
import useStore from '../store/useStore'

// Mock dependencies
vi.mock('../services/api')
vi.mock('../store/useStore')

// Mock react-router-dom navigate
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate
  }
})

describe('RiskAssessment', () => {
  const mockStore = {
    addNotification: vi.fn(),
    setAssessments: vi.fn(),
    assessments: mockApiResponses.assessments
  }

  beforeEach(() => {
    vi.clearAllMocks()
    useStore.mockReturnValue(mockStore)
    api.getAssessments.mockResolvedValue(mockApiResponses.assessments)
  })

  describe('Rendering', () => {
    it('should render page heading', async () => {
      api.getAssessments.mockImplementation(() => new Promise(() => {}))

      renderWithRouter(<RiskAssessment />)
      expect(screen.getByText(/Risk Assessment/i)).toBeInTheDocument()
    })

    it('should show loading state initially', () => {
      api.getAssessments.mockImplementation(() => new Promise(() => {}))

      renderWithRouter(<RiskAssessment />)
      expect(screen.getByText(/Loading assessments/i)).toBeInTheDocument()
    })

    it('should render "New Assessment" button', async () => {
      renderWithRouter(<RiskAssessment />)

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /New Assessment/i })).toBeInTheDocument()
      })
    })

    it('should display list of assessments', async () => {
      renderWithRouter(<RiskAssessment />)

      await waitFor(() => {
        expect(screen.getByText(/Test Assessment 1/i)).toBeInTheDocument()
        expect(screen.getByText(/Test Assessment 2/i)).toBeInTheDocument()
      })
    })
  })

  describe('Create Assessment Form', () => {
    it('should toggle create form visibility', async () => {
      const user = userEvent.setup()
      renderWithRouter(<RiskAssessment />)

      await waitFor(() => {
        expect(screen.queryByPlaceholderText(/Nairobi Water Access/i)).not.toBeInTheDocument()
      })

      const newButton = screen.getByRole('button', { name: /New Assessment/i })
      await user.click(newButton)

      expect(screen.getByPlaceholderText(/Nairobi Water Access/i)).toBeInTheDocument()
    })

    it('should create new assessment with valid data', async () => {
      const user = userEvent.setup()
      const newAssessment = {
        id: 3,
        name: 'New Assessment',
        description: 'New description',
        isPublic: false
      }

      api.createAssessment.mockResolvedValue(newAssessment)
      api.getAssessments.mockResolvedValue([...mockApiResponses.assessments, newAssessment])

      renderWithRouter(<RiskAssessment />)

      // Open form
      await waitFor(() => {
        const newButton = screen.getByRole('button', { name: /New Assessment/i })
        user.click(newButton)
      })

      // Fill form
      await waitFor(async () => {
        const nameInput = screen.getByPlaceholderText(/Nairobi Water Access/i)
        const descInput = screen.getByPlaceholderText(/Describe the scope/i)

        await user.type(nameInput, 'New Assessment')
        await user.type(descInput, 'New description')
      })

      // Submit
      await waitFor(async () => {
        const submitButton = screen.getByRole('button', { name: /Create Assessment/i })
        await user.click(submitButton)
      })

      await waitFor(() => {
        expect(api.createAssessment).toHaveBeenCalledWith('New Assessment', 'New description', false)
        expect(mockNavigate).toHaveBeenCalledWith('/assessment/3')
      })
    })

    it('should show error when name is empty', async () => {
      const user = userEvent.setup()
      renderWithRouter(<RiskAssessment />)

      // Open form
      await waitFor(async () => {
        const newButton = screen.getByRole('button', { name: /New Assessment/i })
        await user.click(newButton)
      })

      // Submit without name
      await waitFor(async () => {
        const submitButton = screen.getByRole('button', { name: /Create Assessment/i })
        await user.click(submitButton)
      })

      await waitFor(() => {
        expect(mockStore.addNotification).toHaveBeenCalledWith(
          expect.objectContaining({
            type: 'error',
            message: expect.stringContaining('name')
          })
        )
      })
    })

    it('should toggle public checkbox', async () => {
      const user = userEvent.setup()
      renderWithRouter(<RiskAssessment />)

      await waitFor(async () => {
        const newButton = screen.getByRole('button', { name: /New Assessment/i })
        await user.click(newButton)
      })

      const checkbox = screen.getByRole('checkbox', { name: /Make this assessment public/i })
      expect(checkbox).not.toBeChecked()

      await user.click(checkbox)
      expect(checkbox).toBeChecked()
    })
  })

  describe('Assessment Management', () => {
    it('should display assessment status badges', async () => {
      renderWithRouter(<RiskAssessment />)

      await waitFor(() => {
        expect(screen.getByText('COMPLETED')).toBeInTheDocument()
        expect(screen.getByText('PENDING')).toBeInTheDocument()
      })
    })

    it('should show "View Results" for completed assessments', async () => {
      renderWithRouter(<RiskAssessment />)

      await waitFor(() => {
        const viewButtons = screen.getAllByRole('button', { name: /View Results/i })
        expect(viewButtons.length).toBeGreaterThan(0)
      })
    })

    it('should show "Run Assessment" for pending assessments', async () => {
      renderWithRouter(<RiskAssessment />)

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Run Assessment/i })).toBeInTheDocument()
      })
    })

    it('should run assessment when button clicked', async () => {
      const user = userEvent.setup()
      const runResult = {
        id: 2,
        status: 'COMPLETED',
        summary: { totalRecords: 50 }
      }

      api.runAssessment.mockResolvedValue(runResult)
      renderWithRouter(<RiskAssessment />)

      await waitFor(async () => {
        const runButton = screen.getByRole('button', { name: /Run Assessment/i })
        await user.click(runButton)
      })

      await waitFor(() => {
        expect(api.runAssessment).toHaveBeenCalledWith(2)
        expect(mockNavigate).toHaveBeenCalledWith('/assessment/2')
      })
    })

    it('should delete assessment with confirmation', async () => {
      const user = userEvent.setup()
      window.confirm = vi.fn(() => true)

      api.deleteAssessment.mockResolvedValue({ message: 'Deleted' })
      renderWithRouter(<RiskAssessment />)

      await waitFor(async () => {
        const deleteButtons = screen.getAllByTitle(/Delete/i)
        await user.click(deleteButtons[0])
      })

      await waitFor(() => {
        expect(window.confirm).toHaveBeenCalled()
        expect(api.deleteAssessment).toHaveBeenCalledWith(1)
        expect(api.getAssessments).toHaveBeenCalled()
      })
    })

    it('should not delete when user cancels confirmation', async () => {
      const user = userEvent.setup()
      window.confirm = vi.fn(() => false)

      renderWithRouter(<RiskAssessment />)

      await waitFor(async () => {
        const deleteButtons = screen.getAllByTitle(/Delete/i)
        await user.click(deleteButtons[0])
      })

      expect(window.confirm).toHaveBeenCalled()
      expect(api.deleteAssessment).not.toHaveBeenCalled()
    })
  })

  describe('Empty State', () => {
    it('should show empty state when no assessments', async () => {
      useStore.mockReturnValue({
        ...mockStore,
        assessments: []
      })
      api.getAssessments.mockResolvedValue([])

      renderWithRouter(<RiskAssessment />)

      await waitFor(() => {
        expect(screen.getByText(/No assessments yet/i)).toBeInTheDocument()
        expect(screen.getByText(/Create your first risk assessment/i)).toBeInTheDocument()
      })
    })

    it('should show create button in empty state', async () => {
      useStore.mockReturnValue({
        ...mockStore,
        assessments: []
      })
      api.getAssessments.mockResolvedValue([])

      renderWithRouter(<RiskAssessment />)

      await waitFor(() => {
        const createButtons = screen.getAllByRole('button', { name: /Create Assessment/i })
        expect(createButtons.length).toBeGreaterThan(0)
      })
    })
  })

  describe('Error Handling', () => {
    it('should handle load error', async () => {
      api.getAssessments.mockRejectedValue(new Error('Load failed'))

      renderWithRouter(<RiskAssessment />)

      await waitFor(() => {
        expect(mockStore.addNotification).toHaveBeenCalledWith(
          expect.objectContaining({
            type: 'error',
            message: expect.stringContaining('Failed to load')
          })
        )
      })
    })

    it('should handle create error', async () => {
      const user = userEvent.setup()
      api.createAssessment.mockRejectedValue(new Error('Create failed'))

      renderWithRouter(<RiskAssessment />)

      await waitFor(async () => {
        const newButton = screen.getByRole('button', { name: /New Assessment/i })
        await user.click(newButton)
      })

      await waitFor(async () => {
        const nameInput = screen.getByPlaceholderText(/Nairobi Water Access/i)
        await user.type(nameInput, 'Test')

        const submitButton = screen.getByRole('button', { name: /Create Assessment/i })
        await user.click(submitButton)
      })

      await waitFor(() => {
        expect(mockStore.addNotification).toHaveBeenCalledWith(
          expect.objectContaining({
            type: 'error'
          })
        )
      })
    })
  })
})
