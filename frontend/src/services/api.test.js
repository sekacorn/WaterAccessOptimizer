/**
 * Tests for API Service
 * Tests API calls, interceptors, and error handling
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'

const { mockClient, mockAxios } = vi.hoisted(() => {
  const client = {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  }

  return {
    mockClient: client,
    mockAxios: {
      create: vi.fn(() => client),
      get: vi.fn(),
      post: vi.fn(),
      delete: vi.fn(),
    },
  }
})

vi.mock('axios', () => ({
  default: mockAxios,
}))

import {
  uploadHydroData,
  uploadCommunityData,
  uploadInfrastructureData,
  getUploads,
  deleteUpload,
  createAssessment,
  getAssessments,
  runAssessment,
  getAssessmentResults,
  deleteAssessment,
  getAllCommunities,
  getAllFacilities,
  exportToExcel,
  exportToPDF,
  getQuotaInfo,
} from './api'

describe('API Service', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  describe('Data Upload APIs', () => {
    it('should upload hydro data with FormData', async () => {
      const mockFile = new File(['test'], 'test.csv', { type: 'text/csv' })
      const mockResponse = { status: 'SUCCESS', rowsProcessed: 100 }

      mockClient.post.mockResolvedValue({ data: mockResponse })

      const result = await uploadHydroData(mockFile)

      expect(mockClient.post).toHaveBeenCalledWith(
        expect.stringContaining('/data/upload/hydro'),
        expect.any(FormData),
        expect.objectContaining({
          headers: { 'Content-Type': 'multipart/form-data' },
        }),
      )
      expect(result).toEqual(mockResponse)
    })

    it('should upload community data', async () => {
      const mockFile = new File(['test'], 'community.csv', { type: 'text/csv' })
      const mockResponse = { status: 'SUCCESS', rowsProcessed: 50 }

      mockClient.post.mockResolvedValue({ data: mockResponse })

      const result = await uploadCommunityData(mockFile)

      expect(mockClient.post).toHaveBeenCalledWith(
        expect.stringContaining('/data/upload/community'),
        expect.any(FormData),
        expect.any(Object),
      )
      expect(result).toEqual(mockResponse)
    })

    it('should upload infrastructure data', async () => {
      const mockFile = new File(['test'], 'infrastructure.csv', { type: 'text/csv' })
      const mockResponse = { status: 'WARNING', rowsProcessed: 30, warningCount: 5 }

      mockClient.post.mockResolvedValue({ data: mockResponse })

      const result = await uploadInfrastructureData(mockFile)

      expect(result).toEqual(mockResponse)
      expect(result.warningCount).toBe(5)
    })

    it('should get uploads with pagination', async () => {
      const mockResponse = {
        uploads: [
          { id: 1, filename: 'test1.csv' },
          { id: 2, filename: 'test2.csv' },
        ],
        totalPages: 1,
      }

      mockClient.get.mockResolvedValue({ data: mockResponse })

      const result = await getUploads(0, 20)

      expect(mockClient.get).toHaveBeenCalledWith(
        expect.stringContaining('/data/uploads'),
        expect.objectContaining({
          params: { page: 0, pageSize: 20 },
        }),
      )
      expect(result.uploads.length).toBe(2)
    })

    it('should delete upload', async () => {
      mockClient.delete.mockResolvedValue({ data: { message: 'Deleted' } })

      await deleteUpload(123)

      expect(mockClient.delete).toHaveBeenCalledWith(
        expect.stringContaining('/data/uploads/123'),
      )
    })
  })

  describe('Risk Assessment APIs', () => {
    it('should create assessment', async () => {
      const mockAssessment = {
        id: 1,
        name: 'Test Assessment',
        description: 'Test description',
        isPublic: false,
      }

      mockClient.post.mockResolvedValue({ data: mockAssessment })

      const result = await createAssessment('Test Assessment', 'Test description', false)

      expect(mockClient.post).toHaveBeenCalledWith(
        expect.stringContaining('/risk/assessments'),
        {
          name: 'Test Assessment',
          description: 'Test description',
          isPublic: false,
        },
      )
      expect(result).toEqual(mockAssessment)
    })

    it('should get assessments', async () => {
      const mockAssessments = [
        { id: 1, name: 'Assessment 1', status: 'COMPLETED' },
        { id: 2, name: 'Assessment 2', status: 'PENDING' },
      ]

      mockClient.get.mockResolvedValue({ data: mockAssessments })

      const result = await getAssessments()

      expect(mockClient.get).toHaveBeenCalledWith(
        expect.stringContaining('/risk/assessments'),
      )
      expect(result.length).toBe(2)
    })

    it('should run assessment', async () => {
      const mockResult = {
        id: 1,
        status: 'COMPLETED',
        summary: { totalRecords: 100, highRiskCount: 20 },
      }

      mockClient.post.mockResolvedValue({ data: mockResult })

      const result = await runAssessment(1)

      expect(mockClient.post).toHaveBeenCalledWith(
        expect.stringContaining('/risk/assessments/1/run'),
      )
      expect(result.summary.totalRecords).toBe(100)
    })

    it('should get assessment results', async () => {
      const mockResults = {
        id: 1,
        name: 'Test Assessment',
        summary: { totalRecords: 100 },
        records: [],
      }

      mockClient.get.mockResolvedValue({ data: mockResults })

      const result = await getAssessmentResults(1)

      expect(mockClient.get).toHaveBeenCalledWith(
        expect.stringContaining('/risk/assessments/1/results'),
        expect.objectContaining({
          params: {},
        }),
      )
      expect(result.summary.totalRecords).toBe(100)
    })

    it('should delete assessment', async () => {
      mockClient.delete.mockResolvedValue({ data: { message: 'Deleted' } })

      await deleteAssessment(1)

      expect(mockClient.delete).toHaveBeenCalledWith(
        expect.stringContaining('/risk/assessments/1'),
      )
    })
  })

  describe('Map APIs', () => {
    it('should get all communities as GeoJSON', async () => {
      const mockGeoJSON = {
        type: 'FeatureCollection',
        features: [
          {
            type: 'Feature',
            geometry: { type: 'Point', coordinates: [-1.2921, 36.8219] },
            properties: { communityName: 'Test Community' },
          },
        ],
      }

      mockClient.get.mockResolvedValue({ data: mockGeoJSON })

      const result = await getAllCommunities()

      expect(mockClient.get).toHaveBeenCalledWith(
        expect.stringContaining('/map/communities'),
      )
      expect(result.type).toBe('FeatureCollection')
      expect(result.features.length).toBe(1)
    })

    it('should get all facilities as GeoJSON', async () => {
      const mockGeoJSON = {
        type: 'FeatureCollection',
        features: [],
      }

      mockClient.get.mockResolvedValue({ data: mockGeoJSON })

      const result = await getAllFacilities()

      expect(mockClient.get).toHaveBeenCalledWith(
        expect.stringContaining('/map/facilities'),
      )
      expect(result.type).toBe('FeatureCollection')
    })
  })

  describe('Export APIs', () => {
    it('should export to Excel with blob response', async () => {
      const mockBlob = new Blob(['test'], { type: 'application/vnd.ms-excel' })
      mockClient.get.mockResolvedValue({ data: mockBlob })

      const result = await exportToExcel(1, 'HIGH')

      expect(mockClient.get).toHaveBeenCalledWith(
        expect.stringContaining('/export/assessments/1/excel'),
        expect.objectContaining({
          params: { riskLevel: 'HIGH' },
          responseType: 'blob',
        }),
      )
      expect(result).toBeInstanceOf(Blob)
    })

    it('should export to Excel without risk filter', async () => {
      const mockBlob = new Blob(['test'], { type: 'application/vnd.ms-excel' })
      mockClient.get.mockResolvedValue({ data: mockBlob })

      await exportToExcel(1, null)

      expect(mockClient.get).toHaveBeenCalledWith(
        expect.stringContaining('/export/assessments/1/excel'),
        expect.objectContaining({
          params: {},
          responseType: 'blob',
        }),
      )
    })

    it('should export to PDF with blob response', async () => {
      const mockBlob = new Blob(['test'], { type: 'application/pdf' })
      mockClient.get.mockResolvedValue({ data: mockBlob })

      const result = await exportToPDF(1, 'MEDIUM')

      expect(mockClient.get).toHaveBeenCalledWith(
        expect.stringContaining('/export/assessments/1/pdf'),
        expect.objectContaining({
          params: { riskLevel: 'MEDIUM' },
          responseType: 'blob',
        }),
      )
      expect(result).toBeInstanceOf(Blob)
    })
  })

  describe('Quota API', () => {
    it('should get quota info', async () => {
      const mockQuota = {
        storageUsedMb: 50,
        storageQuotaMb: 1000,
        uploadsUsed: 10,
        uploadsQuota: 100,
        assessmentsUsed: 5,
        assessmentsQuota: 50,
      }

      mockClient.get.mockResolvedValue({ data: mockQuota })

      const result = await getQuotaInfo()

      expect(mockClient.get).toHaveBeenCalledWith(
        expect.stringContaining('/data/quota'),
      )
      expect(result.storageUsedMb).toBe(50)
      expect(result.uploadsUsed).toBe(10)
    })
  })

  describe('Error Handling', () => {
    it('should handle network errors', async () => {
      const networkError = new Error('Network Error')
      mockClient.get.mockRejectedValue(networkError)

      await expect(getAssessments()).rejects.toThrow('Network Error')
    })

    it('should handle 404 errors', async () => {
      const error = {
        response: {
          status: 404,
          data: { message: 'Not found' },
        },
      }
      mockClient.get.mockRejectedValue(error)

      await expect(getAssessmentResults(999)).rejects.toEqual(error)
    })

    it('should handle 500 errors', async () => {
      const error = {
        response: {
          status: 500,
          data: { message: 'Server error' },
        },
      }
      mockClient.post.mockRejectedValue(error)

      await expect(createAssessment('Test', 'Test')).rejects.toEqual(error)
    })
  })
})
