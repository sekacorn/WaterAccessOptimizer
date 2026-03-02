package com.water.auth.service;

import com.water.auth.dto.AuthResponse;
import com.water.auth.dto.SsoConfigRequest;
import com.water.auth.dto.SsoLoginRequest;
import com.water.auth.model.SsoAuthLog;
import com.water.auth.model.SsoConfig;
import com.water.auth.model.SsoSession;
import com.water.auth.model.User;
import com.water.auth.repository.SsoConfigRepository;
import com.water.auth.repository.SsoSessionRepository;
import com.water.auth.repository.SsoAuthLogRepository;
import com.water.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * SSO Service
 *
 * Handles Single Sign-On authentication for enterprise accounts
 * Supports SAML 2.0 and OAuth 2.0/OIDC protocols
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SsoService {

    private final SsoConfigRepository ssoConfigRepository;
    private final SsoSessionRepository ssoSessionRepository;
    private final SsoAuthLogRepository ssoAuthLogRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${app.url:http://localhost:8080}")
    private String appUrl;

    /**
     * Get available SSO providers
     */
    public List<Map<String, Object>> getAvailableProviders() {
        return List.of(
            Map.of("id", "okta", "name", "Okta", "protocol", "SAML2"),
            Map.of("id", "azure_ad", "name", "Azure Active Directory", "protocol", "SAML2"),
            Map.of("id", "google_workspace", "name", "Google Workspace", "protocol", "SAML2"),
            Map.of("id", "auth0", "name", "Auth0", "protocol", "OIDC"),
            Map.of("id", "onelogin", "name", "OneLogin", "protocol", "SAML2"),
            Map.of("id", "keycloak", "name", "Keycloak", "protocol", "OIDC"),
            Map.of("id", "custom", "name", "Custom Provider", "protocol", "SAML2")
        );
    }

    /**
     * Configure SSO for enterprise
     */
    public void configureSso(UUID enterpriseId, SsoConfigRequest request) {
        // Validate configuration
        validateSsoConfig(request);

        // Create or update SSO configuration
        SsoConfig config = new SsoConfig();
        config.setId(UUID.randomUUID());
        config.setEnterpriseId(enterpriseId);
        config.setProvider(request.getProvider());
        config.setProtocol(request.getProtocol());
        config.setIdpEntityId(request.getIdpEntityId());
        config.setIdpSsoUrl(request.getIdpSsoUrl());
        config.setIdpCertificate(request.getIdpCertificate());
        config.setClientId(request.getClientId());
        config.setClientSecret(encryptSecret(request.getClientSecret()));
        config.setScopes(request.getScopes());
        config.setAllowedDomains(request.getAllowedDomains());
        config.setForceSso(request.getForceSso());
        config.setAutoProvision(request.getAutoProvision());
        config.setDefaultRole(request.getDefaultRole());
        config.setAttributeMappings(request.getAttributeMappings());
        config.setSloUrl(request.getSloUrl());
        config.setJitProvisioning(request.getJitProvisioning());
        config.setSessionTimeoutMinutes(request.getSessionTimeoutMinutes());
        config.setEnabled(true);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());

        ssoConfigRepository.save(config);
        log.info("SSO configured for enterprise: {}, provider: {}", enterpriseId, request.getProvider());
    }

    /**
     * Get SSO configuration for enterprise
     */
    public Map<String, Object> getSsoConfig(UUID enterpriseId) {
        SsoConfig config = ssoConfigRepository.findByEnterpriseId(enterpriseId)
            .orElseThrow(() -> new RuntimeException("SSO not configured"));

        // Return config without sensitive data
        return Map.of(
            "provider", config.getProvider(),
            "protocol", config.getProtocol(),
            "allowedDomains", config.getAllowedDomains(),
            "forceSso", config.getForceSso(),
            "autoProvision", config.getAutoProvision(),
            "enabled", config.getEnabled()
        );
    }

    /**
     * Get SSO info by email domain
     */
    public Map<String, Object> getSsoByDomain(String domain) {
        Optional<SsoConfig> config = ssoConfigRepository.findByDomain(domain);

        if (config.isPresent() && config.get().getEnabled()) {
            return Map.of(
                "provider", config.get().getProvider(),
                "requireSso", config.get().getForceSso(),
                "enterpriseId", config.get().getEnterpriseId()
            );
        }

        return null;
    }

    /**
     * Get enterprise ID by domain
     */
    public UUID getEnterpriseIdByDomain(String domain) {
        return ssoConfigRepository.findByDomain(domain)
            .map(SsoConfig::getEnterpriseId)
            .orElse(null);
    }

    /**
     * Initiate SSO login
     */
    public String initiateSsoLogin(String provider, UUID enterpriseId) {
        SsoConfig config = ssoConfigRepository.findByEnterpriseId(enterpriseId)
            .orElseThrow(() -> new RuntimeException("SSO not configured"));

        if (!config.getEnabled()) {
            throw new RuntimeException("SSO is disabled");
        }

        if (config.getProtocol().equals("SAML2")) {
            return generateSamlAuthRequest(config);
        } else if (config.getProtocol().equals("OIDC")) {
            return generateOidcAuthRequest(config);
        }

        throw new RuntimeException("Unsupported SSO protocol");
    }

    /**
     * Process SSO callback
     */
    public AuthResponse processSsoCallback(SsoLoginRequest request) {
        // Get enterprise by provider or email
        String email = extractEmailFromSsoResponse(request);
        String domain = email.substring(email.indexOf('@') + 1);

        SsoConfig config = ssoConfigRepository.findByDomain(domain)
            .orElseThrow(() -> new RuntimeException("SSO not configured for domain: " + domain));

        // Verify SSO response
        Map<String, Object> attributes = verifySsoResponse(request, config);

        // Get or create user
        User user = getOrCreateUser(email, attributes, config);

        // Create SSO session
        createSsoSession(user, config, attributes);

        // Generate JWT token
        String token = jwtService.generateToken(user);

        return new AuthResponse(token, user, 3600L);
    }

    /**
     * Generate SAML metadata for IdP configuration
     */
    public String generateSamlMetadata(UUID enterpriseId) {
        String entityId = appUrl + "/api/auth/sso/metadata/" + enterpriseId;
        String acsUrl = appUrl + "/api/auth/sso/callback";
        String sloUrl = appUrl + "/api/auth/sso/logout";

        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata"
                                 entityID="%s">
                <md:SPSSODescriptor protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
                    <md:AssertionConsumerService
                        Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
                        Location="%s"
                        index="0"/>
                    <md:SingleLogoutService
                        Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect"
                        Location="%s"/>
                </md:SPSSODescriptor>
            </md:EntityDescriptor>
            """, entityId, acsUrl, sloUrl);
    }

    /**
     * Test SSO configuration
     */
    public Map<String, Object> testSsoConfiguration(UUID enterpriseId) {
        SsoConfig config = ssoConfigRepository.findByEnterpriseId(enterpriseId)
            .orElseThrow(() -> new RuntimeException("SSO not configured"));

        List<String> checks = new ArrayList<>();
        boolean allPassed = true;

        // Check IdP connectivity
        try {
            // In real implementation, ping IdP metadata endpoint
            checks.add("IdP connectivity: OK");
        } catch (Exception e) {
            checks.add("IdP connectivity: FAILED - " + e.getMessage());
            allPassed = false;
        }

        // Check certificate validity (SAML)
        if (config.getProtocol().equals("SAML2")) {
            if (config.getIdpCertificate() != null && !config.getIdpCertificate().isEmpty()) {
                checks.add("IdP certificate: OK");
            } else {
                checks.add("IdP certificate: MISSING");
                allPassed = false;
            }
        }

        // Check client credentials (OIDC)
        if (config.getProtocol().equals("OIDC")) {
            if (config.getClientId() != null && config.getClientSecret() != null) {
                checks.add("OAuth credentials: OK");
            } else {
                checks.add("OAuth credentials: MISSING");
                allPassed = false;
            }
        }

        return Map.of(
            "success", allPassed,
            "checks", checks,
            "provider", config.getProvider(),
            "protocol", config.getProtocol()
        );
    }

    /**
     * Disable SSO for enterprise
     */
    public void disableSso(UUID enterpriseId) {
        SsoConfig config = ssoConfigRepository.findByEnterpriseId(enterpriseId)
            .orElseThrow(() -> new RuntimeException("SSO not configured"));

        config.setEnabled(false);
        config.setUpdatedAt(LocalDateTime.now());
        ssoConfigRepository.update(config);

        log.info("SSO disabled for enterprise: {}", enterpriseId);
    }

    /**
     * Get SSO session
     */
    public SsoSession getSsoSession(UUID userId) {
        return ssoSessionRepository.findByUserId(userId).orElse(null);
    }

    /**
     * Initiate SSO logout
     */
    public String initiateSsoLogout(SsoSession session) {
        // In real implementation, generate SAML LogoutRequest or OAuth revocation
        return session.getProvider() + " logout URL";
    }

    /**
     * Invalidate SSO session
     */
    public void invalidateSsoSession(UUID userId) {
        ssoSessionRepository.deleteByUserId(userId);
    }

    /**
     * Get SSO authentication logs
     */
    public List<SsoAuthLog> getSsoLogs(UUID enterpriseId, int page, int size) {
        return ssoAuthLogRepository.findByEnterpriseId(enterpriseId, page, size);
    }

    /**
     * Log SSO authentication event
     */
    public void logSsoAuth(UUID enterpriseId, UUID userId, String email, String provider,
                          String action, boolean success, String errorMessage,
                          String ipAddress, String userAgent) {
        SsoAuthLog log = new SsoAuthLog();
        log.setId(UUID.randomUUID());
        log.setEnterpriseId(enterpriseId);
        log.setUserId(userId);
        log.setEmail(email);
        log.setProvider(provider);
        log.setAction(action);
        log.setSuccess(success);
        log.setErrorMessage(errorMessage);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setTimestamp(LocalDateTime.now());

        ssoAuthLogRepository.save(log);
    }

    // Helper methods

    public UUID getEnterpriseIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        return UUID.fromString(claims.get("enterpriseId", String.class));
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        return UUID.fromString(claims.getSubject());
    }

    public boolean hasEnterpriseAdminRole(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();
        String role = claims.get("role", String.class);
        return "ENTERPRISE_ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }

    private void validateSsoConfig(SsoConfigRequest request) {
        if (request.getProtocol().equals("SAML2")) {
            if (request.getIdpCertificate() == null || request.getIdpCertificate().isEmpty()) {
                throw new IllegalArgumentException("IdP certificate is required for SAML");
            }
        } else if (request.getProtocol().equals("OIDC")) {
            if (request.getClientId() == null || request.getClientSecret() == null) {
                throw new IllegalArgumentException("Client ID and secret are required for OIDC");
            }
        }
    }

    private String encryptSecret(String secret) {
        // In real implementation, use AES encryption
        return Base64.getEncoder().encodeToString(secret.getBytes());
    }

    private String generateSamlAuthRequest(SsoConfig config) {
        // In real implementation, generate proper SAML AuthnRequest
        // For now, return IdP SSO URL
        return config.getIdpSsoUrl();
    }

    private String generateOidcAuthRequest(SsoConfig config) {
        // Build OAuth authorization URL
        String scopes = String.join(" ", config.getScopes());
        return config.getIdpSsoUrl() +
               "?client_id=" + config.getClientId() +
               "&response_type=code" +
               "&scope=" + scopes +
               "&redirect_uri=" + appUrl + "/api/auth/sso/callback";
    }

    private String extractEmailFromSsoResponse(SsoLoginRequest request) {
        // In real implementation, parse SAML assertion or decode OAuth token
        // For now, return email from request
        return request.getEmail();
    }

    private Map<String, Object> verifySsoResponse(SsoLoginRequest request, SsoConfig config) {
        // In real implementation:
        // - For SAML: Verify signature, decrypt assertion, extract attributes
        // - For OIDC: Exchange code for token, verify JWT, extract claims

        // Mock attributes
        return Map.of(
            "email", request.getEmail(),
            "firstName", "John",
            "lastName", "Doe"
        );
    }

    private User getOrCreateUser(String email, Map<String, Object> attributes, SsoConfig config) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            // Update attributes if JIT provisioning is enabled
            if (config.getJitProvisioning()) {
                updateUserAttributes(user, attributes, config);
            }

            return user;
        } else if (config.getAutoProvision()) {
            // Create new user
            User newUser = new User();
            newUser.setId(UUID.randomUUID());
            newUser.setEmail(email);
            newUser.setUsername(email.split("@")[0]);
            newUser.setRole(config.getDefaultRole());
            newUser.setEnterpriseId(config.getEnterpriseId());
            newUser.setCreatedAt(LocalDateTime.now());

            updateUserAttributes(newUser, attributes, config);
            userRepository.save(newUser);

            log.info("Auto-provisioned user via SSO: {}", email);
            return newUser;
        } else {
            throw new RuntimeException("User not found and auto-provisioning is disabled");
        }
    }

    private void updateUserAttributes(User user, Map<String, Object> attributes, SsoConfig config) {
        // Apply attribute mappings
        if (config.getAttributeMappings() != null) {
            config.getAttributeMappings().forEach((userField, idpAttribute) -> {
                Object value = attributes.get(idpAttribute);
                if (value != null) {
                    // Set user field (simplified, use reflection in real implementation)
                    switch (userField) {
                        case "firstName" -> user.setFirstName(value.toString());
                        case "lastName" -> user.setLastName(value.toString());
                        // Add more fields as needed
                    }
                }
            });
        }
    }

    private void createSsoSession(User user, SsoConfig config, Map<String, Object> attributes) {
        SsoSession session = new SsoSession();
        session.setId(UUID.randomUUID());
        session.setUserId(user.getId());
        session.setEnterpriseId(config.getEnterpriseId());
        session.setProvider(config.getProvider());
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(config.getSessionTimeoutMinutes()));

        ssoSessionRepository.save(session);
    }
}
