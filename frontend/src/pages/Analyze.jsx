/**
 * Analyze Page Component
 *
 * Features:
 * - Upload CSV, JSON, GeoJSON water data
 * - Get AI-powered predictions and recommendations
 * - MBTI-tailored management strategies
 */

import React, { useState } from 'react'
import { Upload, TrendingUp, AlertCircle, CheckCircle } from 'lucide-react'
import { uploadHydroData, uploadCommunityData, uploadInfrastructureData, predictWaterAvailability } from '../services/api'
import './Analyze.css'

const Analyze = ({ mbtiType }) => {
  const [uploadStatus, setUploadStatus] = useState({ hydro: null, community: null, infrastructure: null })
  const [prediction, setPrediction] = useState(null)
  const [loading, setLoading] = useState(false)

  // Handle file upload for different data types
  const handleFileUpload = async (file, dataType) => {
    if (!file) return

    try {
      setLoading(true)
      let result

      if (dataType === 'hydro') {
        result = await uploadHydroData(file)
      } else if (dataType === 'community') {
        result = await uploadCommunityData(file)
      } else {
        result = await uploadInfrastructureData(file)
      }

      setUploadStatus(prev => ({
        ...prev,
        [dataType]: { success: true, message: result.message, count: result.recordsProcessed }
      }))
    } catch (error) {
      setUploadStatus(prev => ({
        ...prev,
        [dataType]: { success: false, message: error.message }
      }))
    } finally {
      setLoading(false)
    }
  }

  // Get AI predictions
  const handleGetPredictions = async () => {
    try {
      setLoading(true)
      // Mock data for demonstration - in production this would use uploaded data
      const mockData = {
        hydro_data: [{ measurement_value: 75, measurement_unit: 'ppm', data_type: 'quality', latitude: 0, longitude: 0 }],
        community_data: [{ population: 5000, water_access_level: 'limited', latitude: 0, longitude: 0 }],
        infrastructure_data: [{ facility_type: 'treatment_plant', capacity: 10000, operational_status: 'operational', latitude: 0, longitude: 0 }],
        mbti_type: mbtiType
      }

      const result = await predictWaterAvailability(mockData)
      setPrediction(result)
    } catch (error) {
      console.error('Prediction error:', error)
      alert('Failed to get predictions. Please ensure data is uploaded.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="analyze-page">
      <div className="container">
        <h1>Water Data Analysis</h1>
        <p className="page-subtitle">Upload data and get AI-powered water management recommendations</p>

        {/* File Upload Section */}
        <div className="upload-section">
          <div className="upload-card card">
            <h3><Upload size={24} /> Hydrological Data</h3>
            <p>Upload USGS or WHO water quality, aquifer levels, rainfall data</p>
            <input
              type="file"
              accept=".csv,.json,.geojson"
              onChange={(e) => handleFileUpload(e.target.files[0], 'hydro')}
              disabled={loading}
            />
            {uploadStatus.hydro && (
              <div className={`status ${uploadStatus.hydro.success ? 'success' : 'error'}`}>
                {uploadStatus.hydro.success ? <CheckCircle size={16} /> : <AlertCircle size={16} />}
                {uploadStatus.hydro.message} {uploadStatus.hydro.count && `(${uploadStatus.hydro.count} records)`}
              </div>
            )}
          </div>

          <div className="upload-card card">
            <h3><Upload size={24} /> Community Data</h3>
            <p>Upload OpenStreetMap community population and water access data</p>
            <input
              type="file"
              accept=".csv,.json,.geojson"
              onChange={(e) => handleFileUpload(e.target.files[0], 'community')}
              disabled={loading}
            />
            {uploadStatus.community && (
              <div className={`status ${uploadStatus.community.success ? 'success' : 'error'}`}>
                {uploadStatus.community.success ? <CheckCircle size={16} /> : <AlertCircle size={16} />}
                {uploadStatus.community.message}
              </div>
            )}
          </div>

          <div className="upload-card card">
            <h3><Upload size={24} /> Infrastructure Data</h3>
            <p>Upload water treatment facilities, pipelines, reservoirs data</p>
            <input
              type="file"
              accept=".csv,.json,.geojson"
              onChange={(e) => handleFileUpload(e.target.files[0], 'infrastructure')}
              disabled={loading}
            />
            {uploadStatus.infrastructure && (
              <div className={`status ${uploadStatus.infrastructure.success ? 'success' : 'error'}`}>
                {uploadStatus.infrastructure.success ? <CheckCircle size={16} /> : <AlertCircle size={16} />}
                {uploadStatus.infrastructure.message}
              </div>
            )}
          </div>
        </div>

        {/* Get Predictions Button */}
        <div className="prediction-section">
          <button
            onClick={handleGetPredictions}
            className="button button-large"
            disabled={loading}
          >
            {loading ? 'Analyzing...' : 'Get AI Predictions'}
            <TrendingUp size={20} />
          </button>
        </div>

        {/* Prediction Results */}
        {prediction && (
          <div className="results-section">
            <div className="result-card card">
              <h2>Water Availability Analysis</h2>
              <div className="score-display">
                <div className="score-circle" style={{
                  background: `conic-gradient(var(--primary) ${prediction.availability_score * 360}deg, #e0e0e0 0deg)`
                }}>
                  <div className="score-inner">
                    <span className="score-value">{(prediction.availability_score * 100).toFixed(0)}%</span>
                    <span className="score-label">Availability</span>
                  </div>
                </div>
                <div className="score-details">
                  <p><strong>Risk Level:</strong> <span className={`risk-${prediction.risk_level}`}>{prediction.risk_level.toUpperCase()}</span></p>
                  <p><strong>Confidence:</strong> {(prediction.confidence * 100).toFixed(0)}%</p>
                </div>
              </div>
            </div>

            <div className="strategies-card card">
              <h3>Management Strategies</h3>
              <ul className="strategies-list">
                {prediction.management_strategies.map((strategy, index) => (
                  <li key={index}>{strategy}</li>
                ))}
              </ul>
            </div>

            {prediction.recommendations && (
              <div className="recommendations-card card">
                <h3>Personalized Recommendations for {mbtiType}</h3>
                <p className="mbti-message">{prediction.recommendations.message}</p>
                <p className="mbti-style">Style: {prediction.recommendations.style}</p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

export default Analyze
