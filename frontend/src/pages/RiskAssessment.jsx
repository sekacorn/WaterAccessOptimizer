/**
 * Risk Assessment Page
 * Create and manage water access risk assessments
 */

import { useState, useEffect, useCallback } from 'react'
import { FileDown, Plus, Trash2, Eye, AlertTriangle, CheckCircle, Clock } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { createAssessment, getAssessments, deleteAssessment, runAssessment } from '../services/api'
import useStore from '../store/useStore'

function RiskAssessment() {
  const navigate = useNavigate()
  const { addNotification, setAssessments, assessments } = useStore()
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    isPublic: false
  })

  const loadAssessments = useCallback(async () => {
    try {
      const data = await getAssessments()
      setAssessments(data || [])
    } catch (error) {
      console.error('Failed to load assessments:', error)
      addNotification({ type: 'error', message: 'Failed to load assessments' })
    } finally {
      setLoading(false)
    }
  }, [addNotification, setAssessments])

  useEffect(() => {
    loadAssessments()
  }, [loadAssessments])

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }))
  }

  const handleCreateAssessment = async (e) => {
    e.preventDefault()

    if (!formData.name.trim()) {
      addNotification({ type: 'error', message: 'Please enter an assessment name' })
      return
    }

    setCreating(true)
    try {
      const newAssessment = await createAssessment(
        formData.name,
        formData.description,
        formData.isPublic
      )

      addNotification({ type: 'success', message: 'Assessment created successfully' })

      // Reset form
      setFormData({ name: '', description: '', isPublic: false })
      setShowCreateForm(false)

      // Reload assessments
      await loadAssessments()

      // Navigate to the new assessment
      navigate(`/assessment/${newAssessment.id}`)
    } catch (error) {
      addNotification({ type: 'error', message: error.message || 'Failed to create assessment' })
    } finally {
      setCreating(false)
    }
  }

  const handleRunAssessment = async (assessmentId) => {
    try {
      addNotification({ type: 'info', message: 'Running risk assessment...' })
      const result = await runAssessment(assessmentId)
      addNotification({ type: 'success', message: `Assessment completed: ${result.summary?.totalRecords || 0} records analyzed` })
      await loadAssessments()
      navigate(`/assessment/${assessmentId}`)
    } catch (error) {
      addNotification({ type: 'error', message: error.message || 'Failed to run assessment' })
    }
  }

  const handleDeleteAssessment = async (assessmentId, assessmentName) => {
    // eslint-disable-next-line no-alert
    if (!window.confirm(`Are you sure you want to delete "${assessmentName}"?`)) {
      return
    }

    try {
      await deleteAssessment(assessmentId)
      addNotification({ type: 'success', message: 'Assessment deleted successfully' })
      await loadAssessments()
    } catch (error) {
      addNotification({ type: 'error', message: 'Failed to delete assessment' })
    }
  }

  const getStatusIcon = (status) => {
    switch (status) {
      case 'COMPLETED':
        return <CheckCircle size={18} className="status-icon success" />
      case 'FAILED':
        return <AlertTriangle size={18} className="status-icon error" />
      case 'RUNNING':
        return <Clock size={18} className="status-icon warning" />
      default:
        return <Clock size={18} className="status-icon" />
    }
  }

  if (loading) {
    return (
      <div className="page">
        <div className="container">
          <h1><FileDown size={32} /> Risk Assessment</h1>
          <p>Loading assessments...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="page">
      <div className="container">
        <div className="page-header">
          <div>
            <h1><FileDown size={32} /> Risk Assessment</h1>
            <p className="page-subtitle">Create and manage water access risk assessments</p>
          </div>
          <button
            onClick={() => setShowCreateForm(!showCreateForm)}
            className="btn-primary"
          >
            <Plus size={18} /> New Assessment
          </button>
        </div>

        {/* Create Assessment Form */}
        {showCreateForm && (
          <div className="card">
            <h2>Create New Assessment</h2>
            <form onSubmit={handleCreateAssessment} noValidate>
              <div className="form-group">
                <label htmlFor="name">Assessment Name *</label>
                <input
                  type="text"
                  id="name"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  placeholder="e.g., Nairobi Water Access 2026"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="description">Description</label>
                <textarea
                  id="description"
                  name="description"
                  value={formData.description}
                  onChange={handleInputChange}
                  placeholder="Describe the scope and objectives of this assessment"
                  rows={4}
                />
              </div>

              <div className="form-group">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    name="isPublic"
                    checked={formData.isPublic}
                    onChange={handleInputChange}
                  />
                  <span>Make this assessment public</span>
                </label>
                <small>Public assessments can be viewed by other users in your organization</small>
              </div>

              <div className="form-actions">
                <button
                  type="submit"
                  disabled={creating}
                  className="btn-primary"
                >
                  {creating ? 'Creating...' : 'Create Assessment'}
                </button>
                <button
                  type="button"
                  onClick={() => setShowCreateForm(false)}
                  className="btn-secondary"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Assessments List */}
        <div className="card">
          <h2>Your Assessments</h2>
          {assessments.length > 0 ? (
            <div className="assessments-grid">
              {assessments.map(assessment => (
                <div key={assessment.id} className="assessment-card">
                  <div className="assessment-header">
                    <h3>{assessment.name}</h3>
                    {getStatusIcon(assessment.status)}
                  </div>

                  {assessment.description && (
                    <p className="assessment-description">{assessment.description}</p>
                  )}

                  <div className="assessment-meta">
                    <div className="meta-item">
                      <strong>Status:</strong>
                      <span className={`status ${assessment.status.toLowerCase()}`}>
                        {assessment.status}
                      </span>
                    </div>
                    <div className="meta-item">
                      <strong>Created:</strong> {new Date(assessment.createdAt).toLocaleDateString()}
                    </div>
                    {assessment.completedAt && (
                      <div className="meta-item">
                        <strong>Completed:</strong> {new Date(assessment.completedAt).toLocaleDateString()}
                      </div>
                    )}
                    {assessment.recordCount !== undefined && (
                      <div className="meta-item">
                        <strong>Records:</strong> {assessment.recordCount}
                      </div>
                    )}
                  </div>

                  <div className="assessment-actions">
                    {assessment.status === 'COMPLETED' ? (
                      <button
                        onClick={() => navigate(`/assessment/${assessment.id}`)}
                        className="btn-primary"
                      >
                        <Eye size={18} /> View Results
                      </button>
                    ) : assessment.status === 'PENDING' ? (
                      <button
                        onClick={() => handleRunAssessment(assessment.id)}
                        className="btn-primary"
                      >
                        <FileDown size={18} /> Run Assessment
                      </button>
                    ) : (
                      <button className="btn-secondary" disabled>
                        <Clock size={18} /> {assessment.status}
                      </button>
                    )}

                    <button
                      onClick={() => handleDeleteAssessment(assessment.id, assessment.name)}
                      className="btn-danger-outline"
                      title="Delete assessment"
                    >
                      <Trash2 size={18} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <FileDown size={64} />
              <h3>No assessments yet</h3>
              <p>Create your first risk assessment to analyze water access data</p>
              <button
                onClick={() => setShowCreateForm(true)}
                className="btn-primary"
              >
                <Plus size={18} /> Create Assessment
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default RiskAssessment
