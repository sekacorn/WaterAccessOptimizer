import { LockKeyhole } from 'lucide-react'

function SecurityCompliance() {
  return (
    <div className="page">
      <div className="container">
        <div className="page-header">
          <div>
            <h1><LockKeyhole size={32} /> Security and Compliance</h1>
            <p className="page-subtitle">
              Secure development and deployment readiness aligned to NIST SSDF, privacy-by-design,
              and accessibility expectations.
            </p>
          </div>
        </div>

        <div className="card">
          <h2>NIST-Oriented Secure Development Practices</h2>
          <ul>
            <li>Environment-based secrets with fail-fast startup checks instead of shipping default secrets.</li>
            <li>JWT validation, authentication rate limiting, and stronger backend configuration defaults.</li>
            <li>CI checks for frontend quality and dependency review to support a secure software supply chain.</li>
            <li>Security headers at the frontend edge to reduce browser-side attack surface.</li>
          </ul>
        </div>

        <div className="card">
          <h2>Operational Expectations Before Production</h2>
          <ul>
            <li>
              Use TLS termination, long random secrets, least-privilege database
              credentials, and environment isolation.
            </li>
            <li>Document incident response, log retention, backup handling, and vulnerability triage ownership.</li>
            <li>Run deployment-specific privacy and accessibility reviews before processing live user data.</li>
          </ul>
        </div>

        <div className="card">
          <h2>Certification Status</h2>
          <p>
            This release is compliance-ready in the sense that it includes implementation hooks and
            documentation needed for later formal assessment. It does not claim NIST certification, GDPR
            compliance determination, or Section 508 certification.
          </p>
        </div>
      </div>
    </div>
  )
}

export default SecurityCompliance
