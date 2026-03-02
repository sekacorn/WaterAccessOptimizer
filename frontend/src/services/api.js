/**
 * API Service for WaterAccessOptimizer
 *
 * Handles all HTTP requests to the backend API Gateway.
 * Uses axios for HTTP client with interceptors for error handling.
 */

import axios from 'axios'

// Get API base URL from environment or use default
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
  const params = { page, pageSize }
  if (dataType) params.dataType = dataType
  const response = await apiClient.get('/data/uploads', { params })
  return response.data
}

/**
 * Delete an upload
 */
export const deleteUpload = async (uploadId) => {
  const response = await apiClient.delete(`/data/uploads/${uploadId}`)
  return response.data
}

/**
 * Get storage quota information
 */
export const getQuotaInfo = async () => {
  const response = await apiClient.get('/data/quota')
  return response.data
}

// ==================== RISK ASSESSMENT API ====================

/**
 * Create a new risk assessment
 */
export const createAssessment = async (name, description, isPublic = false) => {
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
  const response = await apiClient.get('/risk/assessments')
  return response.data
}

/**
 * Get public risk assessments
 */
export const getPublicAssessments = async () => {
  const response = await apiClient.get('/risk/assessments/public')
  return response.data
}

/**
 * Get assessment details
 */
export const getAssessment = async (assessmentId) => {
  const response = await apiClient.get(`/risk/assessments/${assessmentId}`)
  return response.data
}

/**
 * Get assessment results
 */
export const getAssessmentResults = async (assessmentId, riskLevel = null) => {
  const params = riskLevel ? { riskLevel } : {}
  const response = await apiClient.get(`/risk/assessments/${assessmentId}/results`, { params })
  return response.data
}

/**
 * Get assessment summary statistics
 */
export const getAssessmentSummary = async (assessmentId) => {
  const response = await apiClient.get(`/risk/assessments/${assessmentId}/summary`)
  return response.data
}

/**
 * Delete an assessment
 */
export const deleteAssessment = async (assessmentId) => {
  const response = await apiClient.delete(`/risk/assessments/${assessmentId}`)
  return response.data
}

// ==================== MAP/GEOSPATIAL API ====================

/**
 * Get communities near a location
 */
export const getCommunitiesNearby = async (longitude, latitude, radiusKm = 10) => {
  const params = { longitude, latitude, radiusKm }
  const response = await apiClient.get('/map/communities/nearby', { params })
  return response.data
}

/**
 * Get facilities near a location
 */
export const getFacilitiesNearby = async (longitude, latitude, radiusKm = 10) => {
  const params = { longitude, latitude, radiusKm }
  const response = await apiClient.get('/map/facilities/nearby', { params })
  return response.data
}

/**
 * Get water quality measurements near a location
 */
export const getMeasurementsNearby = async (longitude, latitude, radiusKm = 10) => {
  const params = { longitude, latitude, radiusKm }
  const response = await apiClient.get('/map/measurements/nearby', { params })
  return response.data
}

/**
 * Get all communities (GeoJSON)
 */
export const getAllCommunities = async () => {
  const response = await apiClient.get('/map/communities')
  return response.data
}

/**
 * Get all facilities (GeoJSON)
 */
export const getAllFacilities = async () => {
  const response = await apiClient.get('/map/facilities')
  return response.data
}

// ==================== EXPORT API ====================

/**
 * Export assessment to Excel
 */
export const exportToExcel = async (assessmentId, riskLevel = null) => {
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
  const response = await apiClient.get('/data/health')
  return response.data
}

export default apiClient
