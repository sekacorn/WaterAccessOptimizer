import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './SsoSettings.css';

/**
 * SSO Settings Page
 *
 * Allows ENTERPRISE_ADMIN to configure Single Sign-On for their organization
 */
const SsoSettings = () => {
  const [loading, setLoading] = useState(false);
  const [ssoConfig, setSsoConfig] = useState(null);
  const [providers, setProviders] = useState([]);
  const [formData, setFormData] = useState({
    provider: '',
    protocol: 'SAML2',
    idpEntityId: '',
    idpSsoUrl: '',
    idpCertificate: '',
    clientId: '',
    clientSecret: '',
    scopes: ['openid', 'email', 'profile'],
    allowedDomains: [''],
    forceSso: false,
    autoProvision: true,
    defaultRole: 'USER',
    sloUrl: '',
    jitProvisioning: true,
    sessionTimeoutMinutes: 480
  });
  const [testResults, setTestResults] = useState(null);
  const [message, setMessage] = useState(null);

  const API_URL = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080';

  useEffect(() => {
    loadProviders();
    loadSsoConfig();
  }, []);

  const loadProviders = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`${API_URL}/api/auth/sso/providers`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setProviders(response.data);
    } catch (error) {
      console.error('Failed to load SSO providers:', error);
    }
  };

  const loadSsoConfig = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await axios.get(`${API_URL}/api/auth/sso/config`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setSsoConfig(response.data);
      setFormData({
        ...formData,
        ...response.data
      });
    } catch (error) {
      // SSO not configured yet
      console.log('SSO not configured');
    }
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({
      ...formData,
      [name]: type === 'checkbox' ? checked : value
    });
  };

  const handleDomainChange = (index, value) => {
    const newDomains = [...formData.allowedDomains];
    newDomains[index] = value;
    setFormData({ ...formData, allowedDomains: newDomains });
  };

  const addDomain = () => {
    setFormData({
      ...formData,
      allowedDomains: [...formData.allowedDomains, '']
    });
  };

  const removeDomain = (index) => {
    const newDomains = formData.allowedDomains.filter((_, i) => i !== index);
    setFormData({ ...formData, allowedDomains: newDomains });
  };

  const handleScopesChange = (e) => {
    const scopes = e.target.value.split(',').map(s => s.trim());
    setFormData({ ...formData, scopes });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage(null);

    try {
      const token = localStorage.getItem('token');
      const response = await axios.post(
        `${API_URL}/api/auth/sso/configure`,
        formData,
        { headers: { Authorization: `Bearer ${token}` } }
      );

      setMessage({ type: 'success', text: 'SSO configured successfully!' });
      loadSsoConfig();
    } catch (error) {
      setMessage({
        type: 'error',
        text: error.response?.data?.error || 'Failed to configure SSO'
      });
    } finally {
      setLoading(false);
    }
  };

  const testConfiguration = async () => {
    setLoading(true);
    setTestResults(null);

    try {
      const token = localStorage.getItem('token');
      const response = await axios.post(
        `${API_URL}/api/auth/sso/test`,
        {},
        { headers: { Authorization: `Bearer ${token}` } }
      );

      setTestResults(response.data);
    } catch (error) {
      setTestResults({
        success: false,
        checks: [error.response?.data?.error || 'Test failed']
      });
    } finally {
      setLoading(false);
    }
  };

  const disableSso = async () => {
    if (!confirm('Are you sure you want to disable SSO? Users will need to use password login.')) {
      return;
    }

    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      await axios.post(
        `${API_URL}/api/auth/sso/disable`,
        {},
        { headers: { Authorization: `Bearer ${token}` } }
      );

      setMessage({ type: 'success', text: 'SSO disabled successfully' });
      setSsoConfig(null);
      loadSsoConfig();
    } catch (error) {
      setMessage({
        type: 'error',
        text: error.response?.data?.error || 'Failed to disable SSO'
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="sso-settings-container">
      <div className="sso-header">
        <h1>Single Sign-On (SSO) Configuration</h1>
        <p className="subtitle">
          Configure enterprise SSO for centralized authentication
        </p>
      </div>

      {message && (
        <div className={`message ${message.type}`}>
          {message.text}
        </div>
      )}

      <div className="sso-content">
        <div className="sso-info-box">
          <h3>Supported SSO Providers</h3>
          <div className="provider-list">
            {providers.map((provider) => (
              <div key={provider.id} className="provider-item">
                <strong>{provider.name}</strong> - {provider.protocol}
              </div>
            ))}
          </div>
        </div>

        <form onSubmit={handleSubmit} className="sso-form">
          <div className="form-section">
            <h3>Provider Selection</h3>
            <div className="form-group">
              <label htmlFor="provider">SSO Provider *</label>
              <select
                id="provider"
                name="provider"
                value={formData.provider}
                onChange={handleInputChange}
                required
              >
                <option value="">Select Provider</option>
                {providers.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="protocol">Protocol *</label>
              <select
                id="protocol"
                name="protocol"
                value={formData.protocol}
                onChange={handleInputChange}
                required
              >
                <option value="SAML2">SAML 2.0</option>
                <option value="OIDC">OAuth 2.0 / OIDC</option>
              </select>
            </div>
          </div>

          {formData.protocol === 'SAML2' ? (
            <div className="form-section">
              <h3>SAML Configuration</h3>
              <div className="form-group">
                <label htmlFor="idpEntityId">IdP Entity ID *</label>
                <input
                  type="text"
                  id="idpEntityId"
                  name="idpEntityId"
                  value={formData.idpEntityId}
                  onChange={handleInputChange}
                  placeholder="http://www.okta.com/exk..."
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="idpSsoUrl">IdP SSO URL *</label>
                <input
                  type="url"
                  id="idpSsoUrl"
                  name="idpSsoUrl"
                  value={formData.idpSsoUrl}
                  onChange={handleInputChange}
                  placeholder="https://dev-123456.okta.com/app/..."
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="idpCertificate">IdP X.509 Certificate *</label>
                <textarea
                  id="idpCertificate"
                  name="idpCertificate"
                  value={formData.idpCertificate}
                  onChange={handleInputChange}
                  placeholder="-----BEGIN CERTIFICATE-----&#10;...&#10;-----END CERTIFICATE-----"
                  rows="6"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="sloUrl">Single Logout URL</label>
                <input
                  type="url"
                  id="sloUrl"
                  name="sloUrl"
                  value={formData.sloUrl}
                  onChange={handleInputChange}
                  placeholder="https://dev-123456.okta.com/app/.../slo/saml"
                />
              </div>
            </div>
          ) : (
            <div className="form-section">
              <h3>OAuth / OIDC Configuration</h3>
              <div className="form-group">
                <label htmlFor="idpEntityId">Issuer URL *</label>
                <input
                  type="url"
                  id="idpEntityId"
                  name="idpEntityId"
                  value={formData.idpEntityId}
                  onChange={handleInputChange}
                  placeholder="https://dev-123456.okta.com"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="idpSsoUrl">Authorization Endpoint *</label>
                <input
                  type="url"
                  id="idpSsoUrl"
                  name="idpSsoUrl"
                  value={formData.idpSsoUrl}
                  onChange={handleInputChange}
                  placeholder="https://dev-123456.okta.com/oauth2/v1/authorize"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="clientId">Client ID *</label>
                <input
                  type="text"
                  id="clientId"
                  name="clientId"
                  value={formData.clientId}
                  onChange={handleInputChange}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="clientSecret">Client Secret *</label>
                <input
                  type="password"
                  id="clientSecret"
                  name="clientSecret"
                  value={formData.clientSecret}
                  onChange={handleInputChange}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="scopes">Scopes (comma-separated)</label>
                <input
                  type="text"
                  id="scopes"
                  name="scopes"
                  value={formData.scopes.join(', ')}
                  onChange={handleScopesChange}
                  placeholder="openid, email, profile"
                />
              </div>
            </div>
          )}

          <div className="form-section">
            <h3>Domain & User Settings</h3>
            <div className="form-group">
              <label>Allowed Email Domains *</label>
              {formData.allowedDomains.map((domain, index) => (
                <div key={index} className="domain-input-group">
                  <input
                    type="text"
                    value={domain}
                    onChange={(e) => handleDomainChange(index, e.target.value)}
                    placeholder="company.com"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => removeDomain(index)}
                    className="btn-remove"
                    disabled={formData.allowedDomains.length === 1}
                  >
                    Remove
                  </button>
                </div>
              ))}
              <button type="button" onClick={addDomain} className="btn-add">
                + Add Domain
              </button>
            </div>

            <div className="form-group checkbox-group">
              <label>
                <input
                  type="checkbox"
                  name="forceSso"
                  checked={formData.forceSso}
                  onChange={handleInputChange}
                />
                <span>Force SSO (disable password login)</span>
              </label>
            </div>

            <div className="form-group checkbox-group">
              <label>
                <input
                  type="checkbox"
                  name="autoProvision"
                  checked={formData.autoProvision}
                  onChange={handleInputChange}
                />
                <span>Auto-provision users on first login</span>
              </label>
            </div>

            <div className="form-group">
              <label htmlFor="defaultRole">Default Role for New Users</label>
              <select
                id="defaultRole"
                name="defaultRole"
                value={formData.defaultRole}
                onChange={handleInputChange}
              >
                <option value="USER">USER</option>
                <option value="MODERATOR">MODERATOR</option>
              </select>
            </div>

            <div className="form-group checkbox-group">
              <label>
                <input
                  type="checkbox"
                  name="jitProvisioning"
                  checked={formData.jitProvisioning}
                  onChange={handleInputChange}
                />
                <span>Just-In-Time (JIT) provisioning (update user data on each login)</span>
              </label>
            </div>

            <div className="form-group">
              <label htmlFor="sessionTimeoutMinutes">Session Timeout (minutes)</label>
              <input
                type="number"
                id="sessionTimeoutMinutes"
                name="sessionTimeoutMinutes"
                value={formData.sessionTimeoutMinutes}
                onChange={handleInputChange}
                min="30"
                max="1440"
              />
            </div>
          </div>

          <div className="form-actions">
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? 'Saving...' : 'Save Configuration'}
            </button>
            {ssoConfig && (
              <>
                <button
                  type="button"
                  onClick={testConfiguration}
                  className="btn-secondary"
                  disabled={loading}
                >
                  Test Configuration
                </button>
                <button
                  type="button"
                  onClick={disableSso}
                  className="btn-danger"
                  disabled={loading}
                >
                  Disable SSO
                </button>
              </>
            )}
          </div>
        </form>

        {testResults && (
          <div className={`test-results ${testResults.success ? 'success' : 'error'}`}>
            <h3>Test Results</h3>
            <ul>
              {testResults.checks.map((check, index) => (
                <li key={index}>{check}</li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
};

export default SsoSettings;
