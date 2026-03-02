/**
 * Dashboard Page
 * Overview of uploads, assessments, and key metrics
 */

import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Upload, Map, BarChart3, FileDown, TrendingUp, Database } from 'lucide-react'
import { getUploads, getAssessments, getQuotaInfo } from '../services/api'
import useStore from '../store/useStore'

function Dashboard() {
  const { quotaInfo, setQuotaInfo } = useStore()
  const [recentUploads, setRecentUploads] = useState([])
  const [recentAssessments, setRecentAssessments] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const loadDashboardData = async () => {
      try {
        const [uploadsData, assessmentsData, quota] = await Promise.all([
          getUploads(0, 5),
          getAssessments(),
          getQuotaInfo()
        ])

        setRecentUploads(uploadsData.uploads || [])
        setRecentAssessments(assessmentsData.slice(0, 5) || [])
        setQuotaInfo(quota)
      } catch (error) {
        console.error('Failed to load dashboard data:', error)
      } finally {
        setLoading(false)
      }
    }

    loadDashboardData()
  }, [setQuotaInfo])

  if (loading) {
    return <div className="loading">Loading dashboard...</div>
  }

  const quotaPercentage = quotaInfo
    ? (quotaInfo.storageUsedMb / quotaInfo.storageQuotaMb) * 100
    : 0

  return (
    <div className="page dashboard-page">
      <div className="container">
        <h1>Dashboard</h1>
        <p className="page-subtitle">Water Access Risk Assessment Overview</p>

        {/* Quick Stats */}
        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-icon">
              <Upload size={32} />
            </div>
            <div className="stat-info">
              <h3>{recentUploads.length}</h3>
              <p>Recent Uploads</p>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">
              <BarChart3 size={32} />
            </div>
            <div className="stat-info">
              <h3>{recentAssessments.length}</h3>
              <p>Risk Assessments</p>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">
              <Database size={32} />
            </div>
            <div className="stat-info">
              <h3>{quotaInfo ? `${quotaInfo.storageUsedMb.toFixed(1)} MB` : 'N/A'}</h3>
              <p>Storage Used</p>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon">
              <TrendingUp size={32} />
            </div>
            <div className="stat-info">
              <h3>{quotaPercentage.toFixed(0)}%</h3>
              <p>Quota Usage</p>
            </div>
          </div>
        </div>

        {/* Storage Quota */}
        {quotaInfo && (
          <div className="card">
            <h2>Storage Quota</h2>
            <div className="quota-bar">
              <div
                className="quota-fill"
                style={{ width: `${quotaPercentage}%` }}
              ></div>
            </div>
            <p className="quota-text">
              {quotaInfo.storageUsedMb.toFixed(2)} MB / {quotaInfo.storageQuotaMb.toFixed(2)} MB used
            </p>
          </div>
        )}

        {/* Quick Actions */}
        <div className="card">
          <h2>Quick Actions</h2>
          <div className="actions-grid">
            <Link to="/upload" className="action-card">
              <Upload size={48} />
              <h3>Upload Data</h3>
              <p>Import water quality, community, or infrastructure data</p>
            </Link>

            <Link to="/assessment" className="action-card">
              <FileDown size={48} />
              <h3>Create Assessment</h3>
              <p>Calculate risk scores for all communities</p>
            </Link>

            <Link to="/map" className="action-card">
              <Map size={48} />
              <h3>View Map</h3>
              <p>Explore geospatial data on interactive map</p>
            </Link>
          </div>
        </div>

        {/* Recent Activity */}
        <div className="activity-section">
          <div className="card">
            <h2>Recent Uploads</h2>
            {recentUploads.length > 0 ? (
              <ul className="activity-list">
                {recentUploads.map(upload => (
                  <li key={upload.id}>
                    <span className={`badge ${upload.validationStatus.toLowerCase()}`}>
                      {upload.dataType}
                    </span>
                    <span>{upload.filename}</span>
                    <span className="date">{new Date(upload.uploadedAt).toLocaleDateString()}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p>No uploads yet. <Link to="/upload">Upload your first dataset</Link></p>
            )}
          </div>

          <div className="card">
            <h2>Recent Assessments</h2>
            {recentAssessments.length > 0 ? (
              <ul className="activity-list">
                {recentAssessments.map(assessment => (
                  <li key={assessment.id}>
                    <Link to={`/assessment/${assessment.id}`}>{assessment.name}</Link>
                    <span className="date">{new Date(assessment.createdAt).toLocaleDateString()}</span>
                  </li>
                ))}
              </ul>
            ) : (
              <p>No assessments yet. <Link to="/assessment">Create your first assessment</Link></p>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default Dashboard
