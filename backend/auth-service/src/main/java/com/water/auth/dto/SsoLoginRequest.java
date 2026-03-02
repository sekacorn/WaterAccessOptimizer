package com.water.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for SSO Login Callback
 *
 * Handles both SAML and OAuth callback responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SsoLoginRequest {

    /**
     * SSO Provider
     */
    @NotBlank(message = "Provider is required")
    private String provider;

    /**
     * SAML Response (Base64 encoded) or OAuth Authorization Code
     */
    @NotBlank(message = "SSO response is required")
    private String ssoResponse;

    /**
     * Relay State (SAML) or OAuth State parameter
     * Used to prevent CSRF attacks
     */
    private String relayState;

    /**
     * User email (optional, for validation)
     */
    private String email;

    /**
     * OAuth code verifier (PKCE)
     */
    private String codeVerifier;

    /**
     * Redirect URI (OAuth)
     */
    private String redirectUri;
}
