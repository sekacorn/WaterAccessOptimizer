/**
 * MFA Settings Page
 *
 * Features:
 * - Enable/Disable Two-Factor Authentication (2FA)
 * - Setup TOTP with QR code (Google Authenticator, Authy, etc.)
 * - Generate and view backup codes
 * - Manage trusted devices
 * - View MFA activity history
 *
 * Recommended for:
 * - Enterprise users
 * - Administrators and moderators
 * - Users handling sensitive water data
 */

import React, { useState, useEffect } from 'react'
import { Shield, Key, Smartphone, AlertCircle, CheckCircle, Copy, Download } from 'lucide-react'
import './MfaSettings.css'

const MfaSettings = ({ mbtiType }) => {
  const [mfaStatus, setMfaStatus] = useState({
    enabled: false,
    mfaType: 'TOTP',
    verified: false
  })

  const [setupData, setSetupData] = useState(null)
  const [verificationCode, setVerificationCode] = useState('')
  const [backupCodes, setBackupCodes] = useState([])
  const [trustedDevices, setTrustedDevices] = useState([])
  const [loading, setLoading] = useState(false)
  const [showSetup, setShowSetup] = useState(false)

  // Load MFA status on mount
  useEffect(() => {
    loadMfaStatus()
    if (mfaStatus.enabled) {
      loadTrustedDevices()
    }
  }, [])

  const loadMfaStatus = async () => {
    // In production: GET /api/auth/mfa/status
    // Mock data for demonstration
    setMfaStatus({
      enabled: false,
      mfaType: 'TOTP',
      verified: false
    })
  }

  const loadTrustedDevices = async () => {
    // In production: GET /api/auth/mfa/trusted-devices
    // Mock data
    setTrustedDevices([
      { id: 1, deviceName: 'Chrome on Windows', lastUsed: '2024-01-15T10:30:00', ipAddress: '192.168.1.1' },
      { id: 2, deviceName: 'Safari on iPhone', lastUsed: '2024-01-14T15:20:00', ipAddress: '192.168.1.2' }
    ])
  }

  const handleSetupMfa = async () => {
    try {
      setLoading(true)
      // In production: POST /api/auth/mfa/setup
      // This would return QR code and secret

      // Mock setup data
      setSetupData({
        qrCodeUrl: 'https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=otpauth://totp/WaterAccessOptimizer:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=WaterAccessOptimizer',
        secret: 'JBSWY3DPEHPK3PXP',
        backupCodes: ['1234-5678', '2345-6789', '3456-7890', '4567-8901', '5678-9012']
      })

      setShowSetup(true)
    } catch (error) {
      console.error('Setup MFA failed:', error)
      alert('Failed to setup MFA')
    } finally {
      setLoading(false)
    }
  }

  const handleEnableMfa = async () => {
    if (!verificationCode || verificationCode.length !== 6) {
      alert('Please enter a valid 6-digit code')
      return
    }

    try {
      setLoading(true)
      // In production: POST /api/auth/mfa/enable
      // { code: verificationCode }

      // Mock success
      setBackupCodes(setupData.backupCodes)
      setMfaStatus({ ...mfaStatus, enabled: true, verified: true })
      alert('MFA enabled successfully! Save your backup codes.')
    } catch (error) {
      console.error('Enable MFA failed:', error)
      alert('Invalid verification code')
    } finally {
      setLoading(false)
    }
  }

  const handleDisableMfa = async () => {
    const password = prompt('Enter your password to disable MFA:')
    if (!password) return

    try {
      setLoading(true)
      // In production: POST /api/auth/mfa/disable?password=password

      // Mock success
      setMfaStatus({ ...mfaStatus, enabled: false, verified: false })
      setBackupCodes([])
      setShowSetup(false)
      alert('MFA disabled successfully')
    } catch (error) {
      console.error('Disable MFA failed:', error)
      alert('Failed to disable MFA')
    } finally {
      setLoading(false)
    }
  }

  const handleRegenerateBackupCodes = async () => {
    if (!window.confirm('This will invalidate your existing backup codes. Continue?')) {
      return
    }

    try {
      setLoading(true)
      // In production: POST /api/auth/mfa/backup-codes/regenerate

      // Mock new codes
      const newCodes = ['9876-5432', '8765-4321', '7654-3210', '6543-2109', '5432-1098']
      setBackupCodes(newCodes)
      alert('Backup codes regenerated successfully')
    } catch (error) {
      console.error('Regenerate backup codes failed:', error)
      alert('Failed to regenerate backup codes')
    } finally {
      setLoading(false)
    }
  }

  const handleRemoveDevice = async (deviceId) => {
    if (!window.confirm('Remove this trusted device? You will need to verify MFA next time.')) {
      return
    }

    try {
      setLoading(true)
      // In production: DELETE /api/auth/mfa/trusted-devices/{deviceId}

      setTrustedDevices(trustedDevices.filter(d => d.id !== deviceId))
      alert('Trusted device removed')
    } catch (error) {
      console.error('Remove device failed:', error)
      alert('Failed to remove device')
    } finally {
      setLoading(false)
    }
  }

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text)
    alert('Copied to clipboard!')
  }

  const downloadBackupCodes = () => {
    const text = backupCodes.join('\n')
    const blob = new Blob([text], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'water-access-optimizer-backup-codes.txt'
    a.click()
  }

  return (
    <div className="mfa-settings-page">
      <div className="container">
        <h1><Shield size={32} /> Multi-Factor Authentication</h1>
        <p className="page-subtitle">
          Add an extra layer of security to your account
        </p>

        {/* MFA Status Card */}
        <div className="mfa-status-card card">
          <div className="status-header">
            <div className="status-info">
              <h2>Two-Factor Authentication (2FA)</h2>
              <p>
                {mfaStatus.enabled
                  ? '2FA is currently enabled and protecting your account'
                  : 'Enable 2FA to secure your account with an additional verification step'
                }
              </p>
            </div>
            <div className="status-badge-large">
              {mfaStatus.enabled ? (
                <span className="enabled">
                  <CheckCircle size={24} /> Enabled
                </span>
              ) : (
                <span className="disabled">
                  <AlertCircle size={24} /> Disabled
                </span>
              )}
            </div>
          </div>

          <div className="status-actions">
            {!mfaStatus.enabled ? (
              <button
                className="button button-primary"
                onClick={handleSetupMfa}
                disabled={loading}
              >
                <Shield size={20} /> Enable 2FA
              </button>
            ) : (
              <button
                className="button button-danger"
                onClick={handleDisableMfa}
                disabled={loading}
              >
                Disable 2FA
              </button>
            )}
          </div>
        </div>

        {/* Setup Wizard */}
        {showSetup && !mfaStatus.enabled && setupData && (
          <div className="setup-wizard card">
            <h2>Setup Two-Factor Authentication</h2>

            {/* Step 1: Scan QR Code */}
            <div className="setup-step">
              <h3><Smartphone size={20} /> Step 1: Scan QR Code</h3>
              <p>Use an authenticator app (Google Authenticator, Authy, etc.) to scan this QR code:</p>

              <div className="qr-code-container">
                <img src={setupData.qrCodeUrl} alt="MFA QR Code" />
              </div>

              <p className="or-divider">OR</p>

              <div className="manual-entry">
                <p>Manually enter this code:</p>
                <div className="secret-code">
                  <code>{setupData.secret}</code>
                  <button
                    className="button button-sm"
                    onClick={() => copyToClipboard(setupData.secret)}
                  >
                    <Copy size={16} /> Copy
                  </button>
                </div>
              </div>
            </div>

            {/* Step 2: Verify Code */}
            <div className="setup-step">
              <h3><Key size={20} /> Step 2: Verify Code</h3>
              <p>Enter the 6-digit code from your authenticator app:</p>

              <div className="verification-input">
                <input
                  type="text"
                  maxLength="6"
                  placeholder="000000"
                  value={verificationCode}
                  onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, ''))}
                  className="code-input"
                />
                <button
                  className="button button-primary"
                  onClick={handleEnableMfa}
                  disabled={loading || verificationCode.length !== 6}
                >
                  Verify and Enable
                </button>
              </div>
            </div>

            {/* Step 3: Backup Codes */}
            <div className="setup-step">
              <h3><AlertCircle size={20} /> Step 3: Save Backup Codes</h3>
              <p className="warning">
                Save these backup codes in a secure location. Each code can only be used once.
              </p>

              <div className="backup-codes-display">
                {setupData.backupCodes.map((code, index) => (
                  <div key={index} className="backup-code">{code}</div>
                ))}
              </div>

              <div className="backup-actions">
                <button className="button button-sm" onClick={() => copyToClipboard(setupData.backupCodes.join('\n'))}>
                  <Copy size={16} /> Copy All
                </button>
                <button className="button button-sm" onClick={downloadBackupCodes}>
                  <Download size={16} /> Download
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Backup Codes Section (if MFA enabled) */}
        {mfaStatus.enabled && backupCodes.length > 0 && (
          <div className="backup-codes-section card">
            <h2><Key size={24} /> Backup Codes</h2>
            <p>Use these codes if you lose access to your authenticator app. Each code works only once.</p>

            <div className="backup-codes-display">
              {backupCodes.map((code, index) => (
                <div key={index} className="backup-code">{code}</div>
              ))}
            </div>

            <div className="backup-actions">
              <button className="button button-sm" onClick={handleRegenerateBackupCodes}>
                Regenerate Codes
              </button>
              <button className="button button-sm" onClick={downloadBackupCodes}>
                <Download size={16} /> Download
              </button>
            </div>
          </div>
        )}

        {/* Trusted Devices */}
        {mfaStatus.enabled && (
          <div className="trusted-devices-section card">
            <h2>Trusted Devices</h2>
            <p>Devices where you've chosen "Trust this device" won't require MFA for 30 days.</p>

            {trustedDevices.length > 0 ? (
              <div className="devices-list">
                {trustedDevices.map(device => (
                  <div key={device.id} className="device-card">
                    <div className="device-info">
                      <h4>{device.deviceName}</h4>
                      <p>Last used: {new Date(device.lastUsed).toLocaleString()}</p>
                      <p className="device-ip">IP: {device.ipAddress}</p>
                    </div>
                    <button
                      className="button button-sm button-danger"
                      onClick={() => handleRemoveDevice(device.id)}
                    >
                      Remove
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              <p className="no-devices">No trusted devices</p>
            )}
          </div>
        )}

        {/* Info Card */}
        <div className="info-card card">
          <h3><AlertCircle size={20} /> Why Enable 2FA?</h3>
          <ul>
            <li>Protects your account even if your password is compromised</li>
            <li>Required for enterprise accounts and administrative roles</li>
            <li>Secures sensitive water data and community information</li>
            <li>Meets compliance requirements for data protection</li>
            <li>Free and easy to set up with any authenticator app</li>
          </ul>
        </div>
      </div>
    </div>
  )
}

export default MfaSettings
