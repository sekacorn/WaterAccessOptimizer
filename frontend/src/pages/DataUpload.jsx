/**
 * Data Upload Page
 * CSV file upload with validation feedback and history
 */

import { useState, useEffect, useCallback } from 'react'
import { Upload, FileText, AlertCircle, CheckCircle, Trash2, XCircle } from 'lucide-react'
import { uploadHydroData, uploadCommunityData, uploadInfrastructureData, getUploads, deleteUpload } from '../services/api'
import useStore from '../store/useStore'

function DataUpload() {
  const { addNotification, setUploads, uploads } = useStore()
  const [selectedFile, setSelectedFile] = useState(null)
  const [dataType, setDataType] = useState('HYDRO')
  const [uploading, setUploading] = useState(false)
  const [uploadResult, setUploadResult] = useState(null)
  const [dragActive, setDragActive] = useState(false)

  const loadUploads = useCallback(async () => {
    try {
      const response = await getUploads(0, 20)
      setUploads(response.uploads || [])
    } catch (error) {
      console.error('Failed to load uploads:', error)
    }
  }, [setUploads])

  useEffect(() => {
    loadUploads()
  }, [loadUploads])

  const handleFileChange = (e) => {
    const file = e.target.files[0]
    if (file && file.name.endsWith('.csv')) {
      setSelectedFile(file)
      setUploadResult(null)
    } else {
      addNotification({ type: 'error', message: 'Please select a CSV file' })
    }
  }

  const handleDrag = (e) => {
    e.preventDefault()
    e.stopPropagation()
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true)
    } else if (e.type === 'dragleave') {
      setDragActive(false)
    }
  }

  const handleDrop = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setDragActive(false)

    const file = e.dataTransfer.files[0]
    if (file && file.name.endsWith('.csv')) {
      setSelectedFile(file)
      setUploadResult(null)
    } else {
      addNotification({ type: 'error', message: 'Please drop a CSV file' })
    }
  }

  const handleUpload = async () => {
    if (!selectedFile) {
      addNotification({ type: 'error', message: 'Please select a file first' })
      return
    }

    setUploading(true)
    setUploadResult(null)

    try {
      let result
      if (dataType === 'HYDRO') {
        result = await uploadHydroData(selectedFile)
      } else if (dataType === 'COMMUNITY') {
        result = await uploadCommunityData(selectedFile)
      } else {
        result = await uploadInfrastructureData(selectedFile)
      }

      setUploadResult(result)

      if (result.status === 'SUCCESS') {
        addNotification({ type: 'success', message: 'File uploaded successfully!' })
        loadUploads()
        setSelectedFile(null)
      } else {
        addNotification({ type: 'warning', message: 'Upload completed with validation issues' })
      }
    } catch (error) {
      addNotification({ type: 'error', message: error.message || 'Upload failed' })
      setUploadResult({ status: 'FAILED', errors: [{ message: error.message }] })
    } finally {
      setUploading(false)
    }
  }

  const handleDelete = async (uploadId) => {
    // eslint-disable-next-line no-alert
    if (!window.confirm('Are you sure you want to delete this upload?')) {
      return
    }

    try {
      await deleteUpload(uploadId)
      addNotification({ type: 'success', message: 'Upload deleted successfully' })
      loadUploads()
    } catch (error) {
      addNotification({ type: 'error', message: 'Failed to delete upload' })
    }
  }

  return (
    <div className="page">
      <div className="container">
        <h1><Upload size={32} /> Data Upload</h1>
        <p className="page-subtitle">Import water quality, community, or infrastructure data</p>

        {/* Upload Section */}
        <div className="card">
          <h2>Upload CSV File</h2>

          <div className="form-group">
            <label>Data Type</label>
            <select
              value={dataType}
              onChange={(e) => setDataType(e.target.value)}
              className="select-input"
            >
              <option value="HYDRO">Water Quality (Hydro Data)</option>
              <option value="COMMUNITY">Community Demographics</option>
              <option value="INFRASTRUCTURE">Water Infrastructure</option>
            </select>
          </div>

          <div
            className={`upload-dropzone ${dragActive ? 'active' : ''}`}
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
          >
            <FileText size={48} />
            {selectedFile ? (
              <>
                <h3>{selectedFile.name}</h3>
                <p>{(selectedFile.size / 1024).toFixed(2)} KB</p>
              </>
            ) : (
              <>
                <h3>Drag and drop CSV file here</h3>
                <p>or click to browse</p>
              </>
            )}
            <input
              type="file"
              accept=".csv"
              onChange={handleFileChange}
              style={{ display: 'none' }}
              id="file-input"
            />
            <label htmlFor="file-input" className="btn-secondary">
              Choose File
            </label>
          </div>

          <button
            onClick={handleUpload}
            disabled={!selectedFile || uploading}
            className="btn-primary"
          >
            {uploading ? 'Uploading...' : 'Upload File'}
          </button>
        </div>

        {/* Upload Result */}
        {uploadResult && (
          <div className={`card result-${uploadResult.status.toLowerCase()}`}>
            <h2>
              {uploadResult.status === 'SUCCESS' && <CheckCircle size={24} />}
              {uploadResult.status === 'WARNING' && <AlertCircle size={24} />}
              {uploadResult.status === 'FAILED' && <XCircle size={24} />}
              Upload Result
            </h2>

            <div className="result-stats">
              <div className="stat">
                <strong>Rows Processed:</strong> {uploadResult.rowsProcessed || 0}
              </div>
              <div className="stat">
                <strong>Errors:</strong> {uploadResult.errorCount || 0}
              </div>
              <div className="stat">
                <strong>Warnings:</strong> {uploadResult.warningCount || 0}
              </div>
            </div>

            {uploadResult.errors && uploadResult.errors.length > 0 && (
              <div className="validation-issues">
                <h3>Errors</h3>
                <ul>
                  {uploadResult.errors.slice(0, 10).map((error) => (
                    <li key={`${error.row || 'row'}-${error.message}`} className="error">
                      {error.row && `Row ${error.row}: `}{error.message}
                    </li>
                  ))}
                  {uploadResult.errors.length > 10 && (
                    <li>... and {uploadResult.errors.length - 10} more errors</li>
                  )}
                </ul>
              </div>
            )}

            {uploadResult.warnings && uploadResult.warnings.length > 0 && (
              <div className="validation-issues">
                <h3>Warnings</h3>
                <ul>
                  {uploadResult.warnings.slice(0, 5).map((warning) => (
                    <li key={`${warning.row || 'row'}-${warning.message}`} className="warning">
                      {warning.row && `Row ${warning.row}: `}{warning.message}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}

        {/* Upload History */}
        <div className="card">
          <h2>Upload History</h2>
          {uploads.length > 0 ? (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Filename</th>
                  <th>Type</th>
                  <th>Rows</th>
                  <th>Status</th>
                  <th>Uploaded</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {uploads.map(upload => (
                  <tr key={upload.id}>
                    <td>{upload.filename}</td>
                    <td>
                      <span className={`badge ${upload.dataType.toLowerCase()}`}>
                        {upload.dataType}
                      </span>
                    </td>
                    <td>{upload.rowCount}</td>
                    <td>
                      <span className={`status ${upload.validationStatus.toLowerCase()}`}>
                        {upload.validationStatus}
                      </span>
                    </td>
                    <td>{new Date(upload.uploadedAt).toLocaleDateString()}</td>
                    <td>
                      <button
                        onClick={() => handleDelete(upload.id)}
                        className="btn-icon"
                        title="Delete"
                      >
                        <Trash2 size={18} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p>No uploads yet. Upload your first dataset above.</p>
          )}
        </div>
      </div>
    </div>
  )
}

export default DataUpload
