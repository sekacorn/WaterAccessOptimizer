package com.water.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for SSO Configuration
 *
 * Used by ENTERPRISE_ADMIN to configure SSO for their organization
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SsoConfigRequest {

    /**
     * SSO Provider
     * Supported: okta, azure_ad, google_workspace, auth0, onelogin, keycloak, custom
     */
    @NotBlank(message = "Provider is required")
    private String provider;

    /**
     * SSO Protocol
     * SAML2 or OIDC (OAuth 2.0 / OpenID Connect)
     */
    @NotBlank(message = "Protocol is required")
    private String protocol; // SAML2, OIDC

    /**
     * Identity Provider (IdP) Entity ID or Issuer URL
     * For SAML: Entity ID (e.g., "http://www.okta.com/exk...")
     * For OIDC: Issuer URL (e.g., "https://dev-123456.okta.com")
     */
    @NotBlank(message = "IdP entity ID is required")
    private String idpEntityId;

    /**
     * Identity Provider SSO URL
     * For SAML: SSO URL (e.g., "https://dev-123456.okta.com/app/...")
     * For OIDC: Authorization endpoint
     */
    @NotBlank(message = "IdP SSO URL is required")
    private String idpSsoUrl;

    /**
     * Identity Provider X.509 Certificate (SAML only)
     * Used to verify SAML assertions
     */
    private String idpCertificate;

    /**
     * OAuth Client ID (OIDC only)
     */
    private String clientId;

    /**
     * OAuth Client Secret (OIDC only)
     */
    private String clientSecret;

    /**
     * OAuth Scopes (OIDC only)
     * e.g., ["openid", "profile", "email"]
     */
    private List<String> scopes;

    /**
     * Allowed email domains for SSO
     * e.g., ["company.com", "company.org"]
     */
    @NotNull(message = "At least one allowed domain is required")
    private List<String> allowedDomains;

    /**
     * Force SSO for all users in these domains
     * If true, users cannot use password login
     */
    private Boolean forceSso = false;

    /**
     * Auto-provision users on first SSO login
     * Creates user account automatically if it doesn't exist
     */
    private Boolean autoProvision = true;

    /**
     * Default role for auto-provisioned users
     * e.g., "USER", "MODERATOR"
     */
    private String defaultRole = "USER";

    /**
     * Attribute mappings from IdP to application
     * Maps IdP attributes to user fields
     * e.g., {"email": "emailAddress", "firstName": "givenName"}
     */
    private Map<String, String> attributeMappings;

    /**
     * Single Logout URL (SAML only)
     */
    private String sloUrl;

    /**
     * Enable Just-In-Time (JIT) provisioning
     * Updates user attributes on each SSO login
     */
    private Boolean jitProvisioning = true;

    /**
     * Session timeout in minutes
     * After this time, user must re-authenticate via SSO
     */
    private Integer sessionTimeoutMinutes = 480; // 8 hours default
}
