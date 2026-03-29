/**
 * API Service for WaterAccessOptimizer
 *
 * Handles all HTTP requests to the active backend APIs.
 * Uses axios for HTTP client with interceptors for error handling.
 */

import axios from 'axios'
import * as mockApi from './mockApi'

const USE_MOCK_API = import.meta.env.VITE_ENABLE_MOCK_API === 'true'

// Get data API base URL from environment or use default
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8087/api/v1'

// Create axios instance with default configuration
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000, // 30 second timeout
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor - add auth token if available
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor - handle errors globally
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      // Server responded with error status
      console.error('API Error:', error.response.data)

      // Handle specific error codes
      if (error.response.status === 401) {
        // Unauthorized - clear token and redirect to login
        localStorage.removeItem('authToken')
        window.location.href = '/login'
      }
    } else if (error.request) {
      // Request made but no response received
      console.error('Network Error:', error.request)
    } else {
      // Error in request setup
      console.error('Error:', error.message)
    }
    return Promise.reject(error)
  }
)

// ==================== DATA UPLOAD API ====================

/**
 * Upload hydrological data (CSV)
 */
export const uploadHydroData = async (file) => {
  if (USE_MOCK_API) {
    return mockApi.uploadHydroData(file)
  }
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiClient.post('/data/upload/hydro', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
  return response.data
}

/**
 * Upload community data (CSV)
 */
export const uploadCommunityData = async (file) => {
  if (USE_MOCK_API) {
    return mockApi.uploadCommunityData(file)
  }
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiClient.post('/data/upload/community', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
  return response.data
}

/**
 * Upload infrastructure data (CSV)
 */
export const uploadInfrastructureData = async (file) => {
  if (USE_MOCK_API) {
    return mockApi.uploadInfrastructureData(file)
  }
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiClient.post('/data/upload/infrastructure', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
  return response.data
}

/**
 * Get user's upload history
 */
export const getUploads = async (page = 0, pageSize = 20, dataType = null) => {
  if (USE_MOCK_API) {
    return mockApi.getUploads(page, pageSize, dataType)
  }
  const params = { page, pageSize }
  if (dataType) {
    params.dataType = dataType
  }
  const response = await apiClient.get('/data/uploads', { params })
  return response.data
}

/**
 * Delete an upload
 */
export const deleteUpload = async (uploadId) => {
  if (USE_MOCK_API) {
    return mockApi.deleteUpload(uploadId)
  }
  const response = await apiClient.delete(`/data/uploads/${uploadId}`)
  return response.data
}

/**
 * Get storage quota information
 */
export const getQuotaInfo = async () => {
  if (USE_MOCK_API) {
    return mockApi.getQuotaInfo()
  }
  const response = await apiClient.get('/data/quota')
  return response.data
}

// ==================== RISK ASSESSMENT API ====================

/**
 * Create a new risk assessment
 */
export const createAssessment = async (name, description, isPublic = false) => {
  if (USE_MOCK_API) {
    return mockApi.createAssessment(name, description, isPublic)
  }
  const response = await apiClient.post('/risk/assessments', {
    name,
    description,
    isPublic,
  })
  return response.data
}

/**
 * Get user's risk assessments
 */
export const getAssessments = async () => {
  if (USE_MOCK_API) {
    return mockApi.getAssessments()
  }
  const response = await apiClient.get('/risk/assessments')
  return response.data
}

/**
 * Get public risk assessments
 */
export const getPublicAssessments = async () => {
  if (USE_MOCK_API) {
    return mockApi.getPublicAssessments()
  }
  const response = await apiClient.get('/risk/assessments/public')
  return response.data
}

/**
 * Get assessment details
 */
export const getAssessment = async (assessmentId) => {
  if (USE_MOCK_API) {
    return mockApi.getAssessment(assessmentId)
  }
  const response = await apiClient.get(`/risk/assessments/${assessmentId}`)
  return response.data
}

/**
 * Get assessment results
 */
export const getAssessmentResults = async (assessmentId, riskLevel = null) => {
  if (USE_MOCK_API) {
    return mockApi.getAssessmentResults(assessmentId, riskLevel)
  }
  const params = riskLevel ? { riskLevel } : {}
  const response = await apiClient.get(`/risk/assessments/${assessmentId}/results`, { params })
  return response.data
}

/**
 * Get assessment summary statistics
 */
export const getAssessmentSummary = async (assessmentId) => {
  if (USE_MOCK_API) {
    return mockApi.getAssessmentSummary(assessmentId)
  }
  const response = await apiClient.get(`/risk/assessments/${assessmentId}/summary`)
  return response.data
}

/**
 * Delete an assessment
 */
export const deleteAssessment = async (assessmentId) => {
  if (USE_MOCK_API) {
    return mockApi.deleteAssessment(assessmentId)
  }
  const response = await apiClient.delete(`/risk/assessments/${assessmentId}`)
  return response.data
}

export const runAssessment = async (assessmentId) => {
  if (USE_MOCK_API) {
    return mockApi.runAssessment(assessmentId)
  }
  const response = await apiClient.post(`/risk/assessments/${assessmentId}/run`)
  return response.data
}

// ==================== MAP/GEOSPATIAL API ====================

/**
 * Get communities near a location
 */
export const getCommunitiesNearby = async (longitude, latitude, radiusKm = 10) => {
  if (USE_MOCK_API) {
    return mockApi.getCommunitiesNearby(longitude, latitude, radiusKm)
  }
  const params = { longitude, latitude, radiusKm }
  const response = await apiClient.get('/map/communities/nearby', { params })
  return response.data
}

/**
 * Get facilities near a location
 */
export const getFacilitiesNearby = async (longitude, latitude, radiusKm = 10) => {
  if (USE_MOCK_API) {
    return mockApi.getFacilitiesNearby(longitude, latitude, radiusKm)
  }
  const params = { longitude, latitude, radiusKm }
  const response = await apiClient.get('/map/facilities/nearby', { params })
  return response.data
}

/**
 * Get water quality measurements near a location
 */
export const getMeasurementsNearby = async (longitude, latitude, radiusKm = 10) => {
  if (USE_MOCK_API) {
    return mockApi.getMeasurementsNearby(longitude, latitude, radiusKm)
  }
  const params = { longitude, latitude, radiusKm }
  const response = await apiClient.get('/map/measurements/nearby', { params })
  return response.data
}

/**
 * Get all communities (GeoJSON)
 */
export const getAllCommunities = async () => {
  if (USE_MOCK_API) {
    return mockApi.getAllCommunities()
  }
  const response = await apiClient.get('/map/communities')
  return response.data
}

/**
 * Get all facilities (GeoJSON)
 */
export const getAllFacilities = async () => {
  if (USE_MOCK_API) {
    return mockApi.getAllFacilities()
  }
  const response = await apiClient.get('/map/facilities')
  return response.data
}

// ==================== EXPORT API ====================

/**
 * Export assessment to Excel
 */
export const exportToExcel = async (assessmentId, riskLevel = null) => {
  if (USE_MOCK_API) {
    return mockApi.exportToExcel(assessmentId, riskLevel)
  }
  const params = riskLevel ? { riskLevel } : {}
  const response = await apiClient.get(`/export/assessments/${assessmentId}/excel`, {
    params,
    responseType: 'blob',
  })
  return response.data
}

/**
 * Export assessment to PDF
 */
export const exportToPDF = async (assessmentId, riskLevel = null) => {
  if (USE_MOCK_API) {
    return mockApi.exportToPDF(assessmentId, riskLevel)
  }
  const params = riskLevel ? { riskLevel } : {}
  const response = await apiClient.get(`/export/assessments/${assessmentId}/pdf`, {
    params,
    responseType: 'blob',
  })
  return response.data
}

// ==================== HEALTH CHECK ====================

/**
 * Check data service health
 */
export const checkHealth = async () => {
  if (USE_MOCK_API) {
    return mockApi.checkHealth()
  }
  const response = await apiClient.get('/data/health')
  return response.data
}

export default apiClient
