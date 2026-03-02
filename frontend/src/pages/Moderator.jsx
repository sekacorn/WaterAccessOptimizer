/**
 * Moderator Dashboard Page
 *
 * Features for MODERATOR role:
 * - View and resolve user reports
 * - Moderate content (data uploads, visualizations)
 * - Warn, suspend, or ban users
 * - Review moderation actions
 * - Community guidelines enforcement
 */

import React, { useState, useEffect } from 'react'
import { AlertCircle, Flag, UserX, CheckCircle, XCircle } from 'lucide-react'
import './Moderator.css'

const Moderator = ({ mbtiType }) => {
  const [reports, setReports] = useState([])
  const [selectedReport, setSelectedReport] = useState(null)
  const [moderationStats, setModerationStats] = useState({
    pendingReports: 0,
    resolvedToday: 0,
    totalActions: 0
  })

  useEffect(() => {
    loadReports()
    loadStats()
  }, [])

  const loadReports = async () => {
    // In production: GET /api/moderator/reports
    // Mock data
    setReports([
      {
        id: 1,
        reporter: 'user123',
        reportedUser: 'badactor',
        reason: 'Inappropriate data upload',
        description: 'User uploaded fake water quality data',
        status: 'PENDING',
        createdAt: '2024-01-15T10:30:00',
        resourceType: 'data'
      },
      {
        id: 2,
        reporter: 'community_lead',
        reportedUser: 'spammer',
        reason: 'Spam in collaboration chat',
        description: 'User posting promotional content repeatedly',
        status: 'UNDER_REVIEW',
        createdAt: '2024-01-15T09:15:00',
        resourceType: 'comment'
      }
    ])
  }

  const loadStats = async () => {
    // In production: GET /api/moderator/stats
    setModerationStats({
      pendingReports: 12,
      resolvedToday: 8,
      totalActions: 156
    })
  }

  const handleResolveReport = async (reportId, action, notes) => {
    // In production: POST /api/moderator/reports/{reportId}/resolve
    // action: WARN, SUSPEND, BAN, DISMISS
    console.log(`Resolving report ${reportId} with action: ${action}`)
    alert(`Report resolved with action: ${action}\nThis would update in production`)
    loadReports()
    setSelectedReport(null)
  }

  const handleDismissReport = async (reportId) => {
    if (window.confirm('Are you sure you want to dismiss this report?')) {
      handleResolveReport(reportId, 'DISMISS', 'No violation found')
    }
  }

  return (
    <div className="moderator-page">
      <div className="container">
        <h1><Flag size={32} /> Moderator Dashboard</h1>
        <p className="page-subtitle">
          Content moderation and community management
        </p>

        {/* Statistics */}
        <div className="mod-stats">
          <div className="stat-box card">
            <AlertCircle size={32} />
            <h3>{moderationStats.pendingReports}</h3>
            <p>Pending Reports</p>
          </div>
          <div className="stat-box card">
            <CheckCircle size={32} />
            <h3>{moderationStats.resolvedToday}</h3>
            <p>Resolved Today</p>
          </div>
          <div className="stat-box card">
            <Flag size={32} />
            <h3>{moderationStats.totalActions}</h3>
            <p>Total Actions</p>
          </div>
        </div>

        {/* Reports List */}
        <div className="reports-section card">
          <h2><AlertCircle size={24} /> User Reports</h2>

          <div className="filters">
            <button className="filter-btn active">All</button>
            <button className="filter-btn">Pending</button>
            <button className="filter-btn">Under Review</button>
            <button className="filter-btn">Resolved</button>
          </div>

          <div className="reports-list">
            {reports.map(report => (
              <div key={report.id} className="report-card card">
                <div className="report-header">
                  <div>
                    <h3>Report #{report.id}</h3>
                    <span className={`status-badge status-${report.status.toLowerCase()}`}>
                      {report.status}
                    </span>
                  </div>
                  <span className="report-date">{new Date(report.createdAt).toLocaleString()}</span>
                </div>

                <div className="report-details">
                  <p><strong>Reporter:</strong> {report.reporter}</p>
                  <p><strong>Reported User:</strong> {report.reportedUser}</p>
                  <p><strong>Reason:</strong> {report.reason}</p>
                  <p><strong>Description:</strong> {report.description}</p>
                  <p><strong>Resource Type:</strong> {report.resourceType}</p>
                </div>

                <div className="report-actions">
                  <button
                    className="button button-sm"
                    onClick={() => setSelectedReport(report)}
                  >
                    Review
                  </button>
                  <button
                    className="button button-sm button-success"
                    onClick={() => handleDismissReport(report.id)}
                  >
                    <XCircle size={16} /> Dismiss
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Moderation Guidelines */}
        <div className="guidelines-section card">
          <h2>Moderation Guidelines</h2>
          <ul>
            <li><strong>Data Integrity:</strong> Ensure uploaded water data is accurate and sourced properly</li>
            <li><strong>Respectful Communication:</strong> Monitor collaboration sessions for harassment or abuse</li>
            <li><strong>No Spam:</strong> Remove promotional or irrelevant content</li>
            <li><strong>Privacy:</strong> Protect user personal information</li>
            <li><strong>Due Process:</strong> Give users opportunity to explain before taking action</li>
          </ul>
        </div>

        {/* Review Modal */}
        {selectedReport && (
          <div className="modal-overlay" onClick={() => setSelectedReport(null)}>
            <div className="modal-content large" onClick={(e) => e.stopPropagation()}>
              <h2>Review Report #{selectedReport.id}</h2>

              <div className="review-details">
                <p><strong>Reported User:</strong> {selectedReport.reportedUser}</p>
                <p><strong>Reason:</strong> {selectedReport.reason}</p>
                <p><strong>Description:</strong> {selectedReport.description}</p>
              </div>

              <div className="moderation-actions">
                <h3>Take Action</h3>
                <div className="action-buttons-vertical">
                  <button
                    className="button button-warning"
                    onClick={() => handleResolveReport(selectedReport.id, 'WARN', 'User warned for policy violation')}
                  >
                    Warn User
                  </button>
                  <button
                    className="button button-danger"
                    onClick={() => handleResolveReport(selectedReport.id, 'SUSPEND', 'User suspended for 7 days')}
                  >
                    Suspend (7 days)
                  </button>
                  <button
                    className="button button-danger"
                    onClick={() => handleResolveReport(selectedReport.id, 'BAN', 'User permanently banned')}
                  >
                    Permanent Ban
                  </button>
                  <button
                    className="button button-secondary"
                    onClick={() => handleResolveReport(selectedReport.id, 'DISMISS', 'No violation found')}
                  >
                    Dismiss Report
                  </button>
                </div>
              </div>

              <button className="button" onClick={() => setSelectedReport(null)}>
                Cancel
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default Moderator
