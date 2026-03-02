import React, { useState } from 'react';
import axios from 'axios';
import './SsoLoginButton.css';

/**
 * SSO Login Button Component
 *
 * Displays SSO login option when user enters email with SSO-enabled domain
 */
const SsoLoginButton = ({ email, onSsoInitiated }) => {
  const [loading, setLoading] = useState(false);
  const [ssoInfo, setSsoInfo] = useState(null);
  const [error, setError] = useState(null);

  const API_URL = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080';

  // Check if email domain has SSO enabled
  const checkSsoAvailability = async () => {
    if (!email || !email.includes('@')) {
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await axios.post(
        `${API_URL}/api/auth/sso/check-domain`,
        null,
        { params: { email } }
      );

      if (response.data.ssoEnabled) {
        setSsoInfo(response.data);
      } else {
        setSsoInfo(null);
      }
    } catch (err) {
      console.error('Failed to check SSO availability:', err);
      setSsoInfo(null);
    } finally {
      setLoading(false);
    }
  };

  // Initiate SSO login
  const handleSsoLogin = async () => {
    setLoading(true);
    setError(null);

    try {
      const domain = email.substring(email.indexOf('@') + 1);
      const response = await axios.get(
        `${API_URL}/api/auth/sso/login/${ssoInfo.provider}`,
        { params: { email } }
      );

      // Redirect to IdP login page
      if (response.data.redirectUrl) {
        window.location.href = response.data.redirectUrl;
        if (onSsoInitiated) {
          onSsoInitiated();
        }
      }
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to initiate SSO login');
      setLoading(false);
    }
  };

  // Auto-check SSO when email changes
  React.useEffect(() => {
    if (email && email.includes('@')) {
      checkSsoAvailability();
    }
  }, [email]);

  if (!ssoInfo) {
    return null;
  }

  return (
    <div className="sso-login-container">
      <div className="sso-divider">
        <span>OR</span>
      </div>

      <button
        type="button"
        onClick={handleSsoLogin}
        disabled={loading}
        className="sso-login-button"
      >
        {loading ? (
          'Redirecting to SSO...'
        ) : (
          <>
            <svg
              className="sso-icon"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="currentColor"
            >
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
            </svg>
            Continue with SSO ({ssoInfo.provider.replace('_', ' ').toUpperCase()})
          </>
        )}
      </button>

      {ssoInfo.requireSso && (
        <div className="sso-required-notice">
          Your organization requires SSO login
        </div>
      )}

      {error && <div className="sso-error">{error}</div>}
    </div>
  );
};

export default SsoLoginButton;
