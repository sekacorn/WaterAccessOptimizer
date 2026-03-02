import React from 'react'

const Troubleshoot = ({ mbtiType }) => {
  return (
    <div className="container">
      <h1>Help & Troubleshooting</h1>
      <div className="card">
        <h3>Common Issues</h3>
        <ul>
          <li>File upload fails: Ensure your file is in CSV, JSON, or GeoJSON format</li>
          <li>Visualization not loading: Upload data first on the Analyze page</li>
          <li>Predictions unavailable: Check that all data types have been uploaded</li>
        </ul>
      </div>
      <div className="card">
        <h3>Supported File Formats</h3>
        <p><strong>CSV:</strong> Comma-separated values with headers</p>
        <p><strong>JSON:</strong> Standard JSON format</p>
        <p><strong>GeoJSON:</strong> Geographic data in GeoJSON format</p>
      </div>
    </div>
  )
}

export default Troubleshoot
