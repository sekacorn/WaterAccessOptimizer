import { ShieldCheck } from 'lucide-react'

function PrivacyNotice() {
  return (
    <div className="page">
      <div className="container">
        <div className="page-header">
          <div>
            <h1><ShieldCheck size={32} /> Privacy Notice</h1>
            <p className="page-subtitle">
              This notice explains how the current release handles personal and operational data.
            </p>
          </div>
        </div>

        <div className="card">
          <h2>Readiness Position</h2>
          <p>
            WaterAccessOptimizer is being prepared for privacy-by-design and data protection alignment.
            This release does not claim GDPR certification. It documents the controls, operating
            expectations, and process hooks needed to support a more formal compliance program.
          </p>
        </div>

        <div className="card">
          <h2>Data Categories</h2>
          <ul>
            <li>Account data such as name, email address, organization, and role.</li>
            <li>Uploaded operational datasets related to hydrology, communities, and infrastructure.</li>
            <li>Assessment outputs, exports, audit trails, and system telemetry needed for support and security.</li>
          </ul>
        </div>

        <div className="card">
          <h2>Privacy Principles</h2>
          <ul>
            <li>
              Data minimization: collect only the information required to authenticate
              users and run assessments.
            </li>
            <li>
              Purpose limitation: uploaded data is used for water-access analysis,
              reporting, and operational troubleshooting.
            </li>
            <li>
              Storage limitation: retention schedules should be set by the deployment
              owner before production use.
            </li>
            <li>
              Integrity and confidentiality: secrets, transport security, access
              control, and audit logging should be enabled in every environment.
            </li>
            <li>
              Transparency: operators should publish a deployment-specific lawful
              basis, retention schedule, and contact point for privacy requests.
            </li>
          </ul>
        </div>

        <div className="card">
          <h2>Data Subject Request Readiness</h2>
          <p>
            Production operators should define a documented workflow for access, correction, deletion,
            export, and objection requests before processing regulated personal data. The application is
            structured so these workflows can be implemented without major architecture changes.
          </p>
        </div>
      </div>
    </div>
  )
}

export default PrivacyNotice
