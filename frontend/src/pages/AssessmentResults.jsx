/**
 * Assessment Results Page
 * View risk assessment results with charts and data tables
 */

import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { BarChart3, FileSpreadsheet, FileText, Filter, ArrowLeft } from 'lucide-react'
import { Chart as ChartJS, ArcElement, CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend } from 'chart.js'
import { Pie, Bar } from 'react-chartjs-2'
import { getAssessmentResults, exportToExcel, exportToPDF } from '../services/api'
import useStore from '../store/useStore'

// Register Chart.js components
ChartJS.register(ArcElement, CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend)

function AssessmentResults() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { addNotification, setCurrentAssessment } = useStore()
  const [loading, setLoading] = useState(true)
  const [results, setResults] = useState(null)
  const [riskFilter, setRiskFilter] = useState('ALL')
  const [exporting, setExporting] = useState(false)

  const loadResults = useCallback(async () => {
    try {
      const data = await getAssessmentResults(id)
      setResults(data)
      setCurrentAssessment(data)
    } catch (error) {
      console.error('Failed to load results:', error)
      addNotification({ type: 'error', message: 'Failed to load assessment results' })
    } finally {
      setLoading(false)
    }
  }, [addNotification, id, setCurrentAssessment])

  useEffect(() => {
    loadResults()
  }, [loadResults])

  const handleExportExcel = async () => {
    setExporting(true)
    try {
      const blob = await exportToExcel(id, riskFilter === 'ALL' ? null : riskFilter)
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `assessment-${id}-${riskFilter.toLowerCase()}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      addNotification({ type: 'success', message: 'Excel file downloaded successfully' })
    } catch (error) {
      addNotification({ type: 'error', message: 'Failed to export to Excel' })
    } finally {
      setExporting(false)
    }
  }

  const handleExportPDF = async () => {
    setExporting(true)
    try {
      const blob = await exportToPDF(id, riskFilter === 'ALL' ? null : riskFilter)
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `assessment-${id}-${riskFilter.toLowerCase()}.pdf`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      addNotification({ type: 'success', message: 'PDF file downloaded successfully' })
    } catch (error) {
      addNotification({ type: 'error', message: 'Failed to export to PDF' })
    } finally {
      setExporting(false)
    }
  }

  const getRiskColor = (riskLevel) => {
    switch (riskLevel) {
      case 'HIGH':
        return '#e74c3c'
      case 'MEDIUM':
        return '#f39c12'
      case 'LOW':
        return '#27ae60'
      default:
        return '#95a5a6'
    }
  }

  const getFilteredRecords = () => {
    if (!results || !results.records) {
      return []
    }
    if (riskFilter === 'ALL') {
      return results.records
    }
    return results.records.filter(record => record.riskLevel === riskFilter)
  }

  if (loading) {
    return (
      <div className="page">
        <div className="container">
          <h1><BarChart3 size={32} /> Assessment Results</h1>
          <p>Loading results...</p>
        </div>
      </div>
    )
  }

  if (!results) {
    return (
      <div className="page">
        <div className="container">
          <h1><BarChart3 size={32} /> Assessment Results</h1>
          <p>No results found for this assessment.</p>
          <button onClick={() => navigate('/assessment')} className="btn-secondary">
            <ArrowLeft size={18} /> Back to Assessments
          </button>
        </div>
      </div>
    )
  }

  const summary = results.summary || {}
  const records = getFilteredRecords()
  const riskSummaryText = `${summary.highRiskCount || 0} high-risk, ${summary.mediumRiskCount || 0} medium-risk, and ${summary.lowRiskCount || 0} low-risk records out of ${summary.totalRecords || 0} total records.`
  const componentSummaryText = `Average component scores are water quality ${summary.avgWaterQuality || 0}, distance ${summary.avgDistance || 0}, reliability ${summary.avgReliability || 0}, population density ${summary.avgPopulationDensity || 0}, and infrastructure ${summary.avgInfrastructure || 0}.`

  // Prepare chart data
  const pieData = {
    labels: ['High Risk', 'Medium Risk', 'Low Risk'],
    datasets: [
      {
        data: [
          summary.highRiskCount || 0,
          summary.mediumRiskCount || 0,
          summary.lowRiskCount || 0
        ],
        backgroundColor: [
          getRiskColor('HIGH'),
          getRiskColor('MEDIUM'),
          getRiskColor('LOW')
        ],
        borderWidth: 2,
        borderColor: '#fff'
      }
    ]
  }

  const barData = {
    labels: ['Water Quality', 'Distance', 'Reliability', 'Population Density', 'Infrastructure'],
    datasets: [
      {
        label: 'Average Score',
        data: [
          summary.avgWaterQuality || 0,
          summary.avgDistance || 0,
          summary.avgReliability || 0,
          summary.avgPopulationDensity || 0,
          summary.avgInfrastructure || 0
        ],
        backgroundColor: '#3498db',
        borderColor: '#2980b9',
        borderWidth: 1
      }
    ]
  }

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom'
      }
    }
  }

  const barOptions = {
    ...chartOptions,
    scales: {
      y: {
        beginAtZero: true,
        max: 100,
        ticks: {
          callback: (value) => `${value}%`
        }
      }
    }
  }

  return (
    <div className="page">
      <div className="container-fluid">
        <div className="page-header">
          <div>
            <button onClick={() => navigate('/assessment')} className="btn-link">
              <ArrowLeft size={18} /> Back to Assessments
            </button>
            <h1><BarChart3 size={32} /> {results.name || 'Assessment Results'}</h1>
            {results.description && <p className="page-subtitle">{results.description}</p>}
          </div>
          <div className="export-buttons">
            <button
              onClick={handleExportExcel}
              disabled={exporting}
              className="btn-secondary"
            >
              <FileSpreadsheet size={18} /> Export Excel
            </button>
            <button
              onClick={handleExportPDF}
              disabled={exporting}
              className="btn-secondary"
            >
              <FileText size={18} /> Export PDF
            </button>
          </div>
        </div>

        {/* Summary Stats */}
        <div className="stats-grid">
          <div className="stat-card">
            <h3>Total Records</h3>
            <div className="stat-value">{summary.totalRecords || 0}</div>
          </div>
          <div className="stat-card high-risk">
            <h3>High Risk</h3>
            <div className="stat-value">{summary.highRiskCount || 0}</div>
            <div className="stat-percentage">
              {summary.totalRecords ? ((summary.highRiskCount / summary.totalRecords) * 100).toFixed(1) : 0}%
            </div>
          </div>
          <div className="stat-card medium-risk">
            <h3>Medium Risk</h3>
            <div className="stat-value">{summary.mediumRiskCount || 0}</div>
            <div className="stat-percentage">
              {summary.totalRecords ? ((summary.mediumRiskCount / summary.totalRecords) * 100).toFixed(1) : 0}%
            </div>
          </div>
          <div className="stat-card low-risk">
            <h3>Low Risk</h3>
            <div className="stat-value">{summary.lowRiskCount || 0}</div>
            <div className="stat-percentage">
              {summary.totalRecords ? ((summary.lowRiskCount / summary.totalRecords) * 100).toFixed(1) : 0}%
            </div>
          </div>
        </div>

        {/* Charts */}
        <div className="charts-grid">
          <div className="card chart-card">
            <h2>Risk Distribution</h2>
            <p className="chart-summary">{riskSummaryText}</p>
            <div className="chart-container" style={{ height: '300px' }}>
              <Pie aria-hidden="true" data={pieData} options={chartOptions} />
            </div>
          </div>
          <div className="card chart-card">
            <h2>Average Component Scores</h2>
            <p className="chart-summary">{componentSummaryText}</p>
            <div className="chart-container" style={{ height: '300px' }}>
              <Bar aria-hidden="true" data={barData} options={barOptions} />
            </div>
          </div>
        </div>

        {/* Results Table */}
        <div className="card">
          <div className="table-header">
            <h2>Detailed Results</h2>
            <div className="table-controls">
              <div className="filter-group">
                <Filter size={18} />
                <label>Filter by Risk:</label>
                <select
                  value={riskFilter}
                  onChange={(e) => setRiskFilter(e.target.value)}
                  className="select-input"
                >
                  <option value="ALL">All Levels</option>
                  <option value="HIGH">High Risk</option>
                  <option value="MEDIUM">Medium Risk</option>
                  <option value="LOW">Low Risk</option>
                </select>
              </div>
              <div className="record-count">
                Showing {records.length} of {results.records?.length || 0} records
              </div>
            </div>
          </div>

          {records.length > 0 ? (
            <div className="table-responsive">
              <table className="data-table">
                <caption className="table-caption">
                  Assessment records filtered by risk level. {riskSummaryText}
                </caption>
                <thead>
                  <tr>
                    <th scope="col">Location</th>
                    <th scope="col">Risk Level</th>
                    <th scope="col">Risk Score</th>
                    <th scope="col">Water Quality</th>
                    <th scope="col">Distance</th>
                    <th scope="col">Reliability</th>
                    <th scope="col">Population</th>
                    <th scope="col">Infrastructure</th>
                  </tr>
                </thead>
                <tbody>
                  {records.map((record) => (
                    <tr key={record.locationName || record.communityName || `${record.region}-${record.riskLevel}`}>
                      <td>
                        <strong>{record.locationName || record.communityName || 'N/A'}</strong>
                        {record.region && <div className="text-small">{record.region}</div>}
                      </td>
                      <td>
                        <span
                          className={`risk-badge ${record.riskLevel.toLowerCase()}`}
                          style={{ backgroundColor: getRiskColor(record.riskLevel) }}
                        >
                          {record.riskLevel}
                        </span>
                      </td>
                      <td>
                        <strong>{record.riskScore?.toFixed(1) || 'N/A'}</strong>
                      </td>
                      <td>{record.waterQualityScore?.toFixed(1) || 'N/A'}</td>
                      <td>{record.distanceScore?.toFixed(1) || 'N/A'}</td>
                      <td>{record.reliabilityScore?.toFixed(1) || 'N/A'}</td>
                      <td>{record.populationDensityScore?.toFixed(1) || 'N/A'}</td>
                      <td>{record.infrastructureScore?.toFixed(1) || 'N/A'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="empty-state">
              <Filter size={64} />
              <h3>No records match this filter</h3>
              <p>Try selecting a different risk level</p>
            </div>
          )}
        </div>

        {/* Assessment Metadata */}
        <div className="card">
          <h2>Assessment Information</h2>
          <div className="metadata-grid">
            <div className="metadata-item">
              <strong>Assessment ID:</strong> {results.id}
            </div>
            <div className="metadata-item">
              <strong>Created:</strong> {new Date(results.createdAt).toLocaleString()}
            </div>
            {results.completedAt && (
              <div className="metadata-item">
                <strong>Completed:</strong> {new Date(results.completedAt).toLocaleString()}
              </div>
            )}
            <div className="metadata-item">
              <strong>Status:</strong>
              <span className={`status ${results.status?.toLowerCase()}`}>
                {results.status}
              </span>
            </div>
            {results.isPublic !== undefined && (
              <div className="metadata-item">
                <strong>Visibility:</strong> {results.isPublic ? 'Public' : 'Private'}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default AssessmentResults
