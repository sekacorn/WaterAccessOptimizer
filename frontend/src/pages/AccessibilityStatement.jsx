import { Accessibility } from 'lucide-react'

function AccessibilityStatement() {
  return (
    <div className="page">
      <div className="container">
        <div className="page-header">
          <div>
            <h1><Accessibility size={32} /> Accessibility Statement</h1>
            <p className="page-subtitle">
              This release is being improved toward Section 508 and WCAG 2.1 AA readiness.
            </p>
          </div>
        </div>

        <div className="card">
          <h2>Accessibility Features In This Release</h2>
          <ul>
            <li>Keyboard-accessible navigation, visible focus styling, and a skip link to main content.</li>
            <li>Live regions for system notifications and error feedback.</li>
            <li>Accessible labels, instructions, and tabular summaries for upload and assessment workflows.</li>
            <li>Reduced-motion support for users who request less animation.</li>
          </ul>
        </div>

        <div className="card">
          <h2>Known Gaps</h2>
          <ul>
            <li>
              Chart canvases still rely on accompanying text summaries rather than
              fully interactive accessible chart alternatives.
            </li>
            <li>
              Map interactions remain partially dependent on visual cues and should
              receive a stronger non-visual equivalent in a future release.
            </li>
            <li>
              Manual assistive technology testing should still be completed before
              any formal accessibility claim is made.
            </li>
          </ul>
        </div>

        <div className="card">
          <h2>Feedback Process</h2>
          <p>
            Before release, the deployment owner should publish an accessibility contact path and a response
            SLA for reported barriers. That process is part of Section 508 readiness even when certification
            is not being pursued.
          </p>
        </div>
      </div>
    </div>
  )
}

export default AccessibilityStatement
