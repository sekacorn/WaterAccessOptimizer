/**
 * Tests for API Service
 * Tests API calls, interceptors, and error handling
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import axios from 'axios'
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
  getQuotaInfo
} from './api'

// Mock axios
vi.mock('axios')

describe('API Service', () => {
  beforeEach(() => {
    // Reset mocks before each test
    vi.clearAllMocks()
    localStorage.clear()
  })

  describe('Data Upload APIs', () => {
    it('should upload hydro data with FormData', async () => {
      const mockFile = new File(['test'], 'test.csv', { type: 'text/csv' })
      const mockResponse = { status: 'SUCCESS', rowsProcessed: 100 }

      axios.post.mockResolvedValue({ data: mockResponse })

      const result = await uploadHydroData(mockFile)

      expect(axios.post).toHaveBeenCalledWith(
        expect.stringContaining('/data/upload/hydro'),
        expect.any(FormData),
        expect.objectContaining({
          headers: { 'Content-Type': 'multipart/form-data' }
        })
      )
      expect(result).toEqual(mockResponse)
    })

    it('should upload community data', async () => {
      const mockFile = new File(['test'], 'community.csv', { type: 'text/csv' })
      const mockResponse = { status: 'SUCCESS', rowsProcessed: 50 }

      axios.post.mockResolvedValue({ data: mockResponse })

      const result = await uploadCommunityData(mockFile)

      expect(axios.post).toHaveBeenCalledWith(
        expect.stringContaining('/data/upload/community'),
        expect.any(FormData),
        expect.any(Object)
      )
      expect(result).toEqual(mockResponse)
    })

    it('should upload infrastructure data', async () => {
      const mockFile = new File(['test'], 'infrastructure.csv', { type: 'text/csv' })
      const mockResponse = { status: 'WARNING', rowsProcessed: 30, warningCount: 5 }

      axios.post.mockResolvedValue({ data: mockResponse })

      const result = await uploadInfrastructureData(mockFile)

      expect(result).toEqual(mockResponse)
      expect(result.warningCount).toBe(5)
    })

    it('should get uploads with pagination', async () => {
      const mockResponse = {
        uploads: [
          { id: 1, filename: 'test1.csv' },
          { id: 2, filename: 'test2.csv' }
        ],
        totalPages: 1
      }

      axios.get.mockResolvedValue({ data: mockResponse })

      const result = await getUploads(0, 20)

      expect(axios.get).toHaveBeenCalledWith(
        expect.stringContaining('/data/uploads'),
        expect.objectContaining({
          params: { page: 0, size: 20 }
        })
      )
      expect(result.uploads.length).toBe(2)
    })

    it('should delete upload', async () => {
      axios.delete.mockResolvedValue({ data: { message: 'Deleted' } })

      await deleteUpload(123)

      expect(axios.delete).toHaveBeenCalledWith(
        expect.stringContaining('/data/uploads/123')
      )
    })
  })

  describe('Risk Assessment APIs', () => {
    it('should create assessment', async () => {
      const mockAssessment = {
        id: 1,
        name: 'Test Assessment',
        description: 'Test description',
        isPublic: false
      }

      axios.post.mockResolvedValue({ data: mockAssessment })

      const result = await createAssessment('Test Assessment', 'Test description', false)

      expect(axios.post).toHaveBeenCalledWith(
        expect.stringContaining('/risk/assessments'),
        {
          name: 'Test Assessment',
          description: 'Test description',
          isPublic: false
        }
      )
      expect(result).toEqual(mockAssessment)
    })

    it('should get assessments', async () => {
      const mockAssessments = [
        { id: 1, name: 'Assessment 1', status: 'COMPLETED' },
        { id: 2, name: 'Assessment 2', status: 'PENDING' }
      ]

      axios.get.mockResolvedValue({ data: mockAssessments })

      const result = await getAssessments()

      expect(axios.get).toHaveBeenCalledWith(
        expect.stringContaining('/risk/assessments')
      )
      expect(result.length).toBe(2)
    })

    it('should run assessment', async () => {
      const mockResult = {
        id: 1,
        status: 'COMPLETED',
        summary: { totalRecords: 100, highRiskCount: 20 }
      }

      axios.post.mockResolvedValue({ data: mockResult })

      const result = await runAssessment(1)

      expect(axios.post).toHaveBeenCalledWith(
        expect.stringContaining('/risk/assessments/1/run')
      )
      expect(result.summary.totalRecords).toBe(100)
    })

    it('should get assessment results', async () => {
      const mockResults = {
        id: 1,
        name: 'Test Assessment',
        summary: { totalRecords: 100 },
        records: []
      }

      axios.get.mockResolvedValue({ data: mockResults })

      const result = await getAssessmentResults(1)

      expect(axios.get).toHaveBeenCalledWith(
        expect.stringContaining('/risk/assessments/1/results')
      )
      expect(result.summary.totalRecords).toBe(100)
    })

    it('should delete assessment', async () => {
      axios.delete.mockResolvedValue({ data: { message: 'Deleted' } })

      await deleteAssessment(1)

      expect(axios.delete).toHaveBeenCalledWith(
        expect.stringContaining('/risk/assessments/1')
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
            properties: { communityName: 'Test Community' }
          }
        ]
      }

      axios.get.mockResolvedValue({ data: mockGeoJSON })

      const result = await getAllCommunities()

      expect(axios.get).toHaveBeenCalledWith(
        expect.stringContaining('/map/communities')
      )
      expect(result.type).toBe('FeatureCollection')
      expect(result.features.length).toBe(1)
    })

    it('should get all facilities as GeoJSON', async () => {
      const mockGeoJSON = {
        type: 'FeatureCollection',
        features: []
      }

      axios.get.mockResolvedValue({ data: mockGeoJSON })

      const result = await getAllFacilities()

      expect(axios.get).toHaveBeenCalledWith(
        expect.stringContaining('/map/facilities')
      )
      expect(result.type).toBe('FeatureCollection')
    })
  })

  describe('Export APIs', () => {
    it('should export to Excel with blob response', async () => {
      const mockBlob = new Blob(['test'], { type: 'application/vnd.ms-excel' })
      axios.get.mockResolvedValue({ data: mockBlob })

      const result = await exportToExcel(1, 'HIGH')

      expect(axios.get).toHaveBeenCalledWith(
        expect.stringContaining('/export/assessments/1/excel'),
        expect.objectContaining({
          params: { riskLevel: 'HIGH' },
          responseType: 'blob'
        })
      )
      expect(result).toBeInstanceOf(Blob)
    })

    it('should export to Excel without risk filter', async () => {
      const mockBlob = new Blob(['test'], { type: 'application/vnd.ms-excel' })
      axios.get.mockResolvedValue({ data: mockBlob })

      await exportToExcel(1, null)

      expect(axios.get).toHaveBeenCalledWith(
        expect.stringContaining('/export/assessments/1/excel'),
        expect.objectContaining({
          params: {},
          responseType: 'blob'
        })
      )
    })

    it('should export to PDF with blob response', async () => {
      const mockBlob = new Blob(['test'], { type: 'application/pdf' })
      axios.get.mockResolvedValue({ data: mockBlob })

      const result = await exportToPDF(1, 'MEDIUM')

      expect(axios.get).toHaveBeenCalledWith(
        expect.stringContaining('/export/assessments/1/pdf'),
        expect.objectContaining({
          params: { riskLevel: 'MEDIUM' },
          responseType: 'blob'
        })
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
        assessmentsQuota: 50
      }

      axios.get.mockResolvedValue({ data: mockQuota })

      const result = await getQuotaInfo()

      expect(axios.get).toHaveBeenCalledWith(
        expect.stringContaining('/user/quota')
      )
      expect(result.storageUsedMb).toBe(50)
      expect(result.uploadsUsed).toBe(10)
    })
  })

  describe('Error Handling', () => {
    it('should handle network errors', async () => {
      const networkError = new Error('Network Error')
      axios.get.mockRejectedValue(networkError)

      await expect(getAssessments()).rejects.toThrow('Network Error')
    })

    it('should handle 404 errors', async () => {
      const error = {
        response: {
          status: 404,
          data: { message: 'Not found' }
        }
      }
      axios.get.mockRejectedValue(error)

      await expect(getAssessmentResults(999)).rejects.toEqual(error)
    })

    it('should handle 500 errors', async () => {
      const error = {
        response: {
          status: 500,
          data: { message: 'Server error' }
        }
      }
      axios.post.mockRejectedValue(error)

      await expect(createAssessment('Test', 'Test')).rejects.toEqual(error)
    })
  })
})
