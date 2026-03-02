import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import axios from 'axios';
import './SsoCallback.css';

/**
 * SSO Callback Handler
 *
 * Processes SSO response from Identity Provider and completes authentication
 */
const SsoCallback = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState('processing');
  const [error, setError] = useState(null);

  const API_URL = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080';

  useEffect(() => {
    handleSsoCallback();
  }, []);

  const handleSsoCallback = async () => {
    try {
      // Extract SSO response from URL parameters
      const samlResponse = searchParams.get('SAMLResponse');
      const code = searchParams.get('code'); // OAuth code
      const state = searchParams.get('state');
      const provider = searchParams.get('provider') || 'okta';
      const error = searchParams.get('error');

      if (error) {
        throw new Error(`SSO error: ${error}`);
      }

      if (!samlResponse && !code) {
        throw new Error('Missing SSO response');
      }

      setStatus('verifying');

      // Send SSO response to backend
      const response = await axios.post(`${API_URL}/api/auth/sso/callback`, {
        provider,
        ssoResponse: samlResponse || code,
        relayState: state,
        email: searchParams.get('email')
      });

      // Store authentication token
      if (response.data.token) {
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('user', JSON.stringify(response.data.user));

        setStatus('success');

        // Redirect to dashboard after brief success message
        setTimeout(() => {
          navigate('/');
        }, 1500);
      } else {
        throw new Error('No token received');
      }
    } catch (err) {
      console.error('SSO callback error:', err);
      setStatus('error');
      setError(err.response?.data?.error || err.message || 'SSO authentication failed');

      // Redirect to login after showing error
      setTimeout(() => {
        navigate('/login');
      }, 3000);
    }
  };

  return (
    <div className="sso-callback-container">
      <div className="sso-callback-content">
        {status === 'processing' && (
          <>
            <div className="spinner"></div>
            <h2>Processing SSO login...</h2>
            <p>Please wait while we authenticate you.</p>
          </>
        )}

        {status === 'verifying' && (
          <>
            <div className="spinner"></div>
            <h2>Verifying credentials...</h2>
            <p>Confirming your identity with the identity provider.</p>
          </>
        )}

        {status === 'success' && (
          <>
            <div className="success-icon">✓</div>
            <h2>SSO Login Successful!</h2>
            <p>Redirecting you to the dashboard...</p>
          </>
        )}

        {status === 'error' && (
          <>
            <div className="error-icon">✗</div>
            <h2>SSO Login Failed</h2>
            <p>{error}</p>
            <p className="redirect-notice">Redirecting to login page...</p>
          </>
        )}
      </div>
    </div>
  );
};

export default SsoCallback;
