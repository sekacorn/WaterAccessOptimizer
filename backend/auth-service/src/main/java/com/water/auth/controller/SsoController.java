package com.water.auth.controller;

import com.water.auth.dto.SsoConfigRequest;
import com.water.auth.dto.SsoLoginRequest;
import com.water.auth.service.SsoService;
import com.water.auth.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Single Sign-On (SSO) Controller
 *
 * Supports multiple SSO protocols and providers:
 *
 * **SAML 2.0:**
 * - Okta
 * - Azure Active Directory (Azure AD)
 * - Google Workspace
 * - OneLogin
 *
 * **OAuth 2.0 / OpenID Connect (OIDC):**
 * - Auth0
 * - Keycloak
 * - Custom OAuth providers
 *
 * **Features:**
 * - Auto-provision users on first SSO login
 * - Force SSO for enterprise users
 * - Single Logout (SLO)
 * - Attribute mapping from IdP
 * - Domain restrictions
 * - SSO session management
 *
 * **Enterprise Benefits:**
 * - Centralized user management
 * - No password management needed
 * - Simplified onboarding/offboarding
 * - Enhanced security with IdP MFA
 * - Compliance with corporate IT policies
 */
@RestController
@RequestMapping("/api/auth/sso")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SsoController {

    private final SsoService ssoService;
    private final AuditLogService auditLogService;

    /**
     * Get available SSO providers
     *
     * GET /api/auth/sso/providers
     *
     * @return List of supported SSO providers
     */
    @GetMapping("/providers")
    public ResponseEntity<?> getProviders() {
        try {
            var providers = ssoService.getAvailableProviders();
            return ResponseEntity.ok(providers);
        } catch (Exception e) {
            log.error("Get SSO providers failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to load SSO providers"));
        }
    }

    /**
     * Configure SSO for enterprise (ENTERPRISE_ADMIN only)
     *
     * POST /api/auth/sso/configure
     *
     * Requires: Authorization header with ENTERPRISE_ADMIN token
     *
     * @param request SSO configuration details
     * @return Success message
     */
    @PostMapping("/configure")
    public ResponseEntity<?> configureSso(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody SsoConfigRequest request,
            HttpServletRequest httpRequest) {
        try {
            String token = authHeader.replace("Bearer ", "");
            UUID enterpriseId = ssoService.getEnterpriseIdFromToken(token);

            log.info("SSO configuration for enterprise: {}", enterpriseId);

            // Validate user has ENTERPRISE_ADMIN role
            if (!ssoService.hasEnterpriseAdminRole(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Requires ENTERPRISE_ADMIN role"));
            }

            // Save SSO configuration
            ssoService.configureSso(enterpriseId, request);

            // Log audit trail
            auditLogService.logAction(
                ssoService.getUserIdFromToken(token),
                "SSO_CONFIGURED",
                "sso_config",
                enterpriseId,
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                "INFO"
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "SSO configured successfully",
                "provider", request.getProvider()
            ));
        } catch (Exception e) {
            log.error("SSO configuration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get SSO configuration for enterprise
     *
     * GET /api/auth/sso/config
     *
     * Requires: Authorization header with ENTERPRISE_ADMIN token
     *
     * @return SSO configuration (without sensitive data)
     */
    @GetMapping("/config")
    public ResponseEntity<?> getSsoConfig(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            UUID enterpriseId = ssoService.getEnterpriseIdFromToken(token);

            var config = ssoService.getSsoConfig(enterpriseId);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("Get SSO config failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "SSO not configured"));
        }
    }

    /**
     * Check if email domain has SSO enabled
     *
     * POST /api/auth/sso/check-domain
     *
     * @param email User email address
     * @return SSO availability for domain
     */
    @PostMapping("/check-domain")
    public ResponseEntity<?> checkDomain(@RequestParam String email) {
        try {
            String domain = email.substring(email.indexOf('@') + 1);
            var ssoInfo = ssoService.getSsoByDomain(domain);

            if (ssoInfo != null) {
                return ResponseEntity.ok(Map.of(
                    "ssoEnabled", true,
                    "provider", ssoInfo.get("provider"),
                    "requireSso", ssoInfo.get("requireSso")
                ));
            } else {
                return ResponseEntity.ok(Map.of("ssoEnabled", false));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("ssoEnabled", false));
        }
    }

    /**
     * Initiate SSO login (SAML/OAuth)
     *
     * GET /api/auth/sso/login/{provider}
     *
     * Redirects to IdP login page
     *
     * @param provider SSO provider (okta, azure_ad, google_workspace, etc.)
     * @param enterpriseId Enterprise ID (optional, can be derived from email domain)
     * @return Redirect to IdP
     */
    @GetMapping("/login/{provider}")
    public ResponseEntity<?> initiateSsoLogin(
            @PathVariable String provider,
            @RequestParam(required = false) UUID enterpriseId,
            @RequestParam(required = false) String email,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            log.info("SSO login initiated for provider: {}", provider);

            // Determine enterprise from email domain if not provided
            if (enterpriseId == null && email != null) {
                String domain = email.substring(email.indexOf('@') + 1);
                enterpriseId = ssoService.getEnterpriseIdByDomain(domain);
            }

            if (enterpriseId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Enterprise not found for SSO"));
            }

            // Generate SSO request (SAML AuthnRequest or OAuth authorization URL)
            String redirectUrl = ssoService.initiateSsoLogin(provider, enterpriseId);

            // Log SSO attempt
            auditLogService.logAction(
                null,
                "SSO_LOGIN_INITIATED",
                "sso",
                enterpriseId,
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                "INFO"
            );

            return ResponseEntity.ok(Map.of(
                "redirectUrl", redirectUrl,
                "provider", provider
            ));
        } catch (Exception e) {
            log.error("SSO login initiation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Handle SSO callback (SAML assertion or OAuth callback)
     *
     * POST /api/auth/sso/callback
     *
     * Processes response from IdP and creates user session
     *
     * @param request SAML response or OAuth code
     * @return JWT token and user info
     */
    @PostMapping("/callback")
    public ResponseEntity<?> handleSsoCallback(
            @Valid @RequestBody SsoLoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            log.info("SSO callback received from provider: {}", request.getProvider());

            // Verify and process SSO response
            var authResponse = ssoService.processSsoCallback(request);

            // Log successful SSO login
            auditLogService.logAction(
                authResponse.getUser().getId(),
                "SSO_LOGIN_SUCCESS",
                "user",
                authResponse.getUser().getId(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                "INFO"
            );

            // Log SSO auth event
            ssoService.logSsoAuth(
                authResponse.getUser().getEnterpriseId(),
                authResponse.getUser().getId(),
                authResponse.getUser().getEmail(),
                request.getProvider(),
                "login",
                true,
                null,
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
            );

            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            log.error("SSO callback failed: {}", e.getMessage());

            // Log failed SSO login
            ssoService.logSsoAuth(
                null,
                null,
                request.getEmail(),
                request.getProvider(),
                "login",
                false,
                e.getMessage(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
            );

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "SSO authentication failed: " + e.getMessage()));
        }
    }

    /**
     * Get SAML metadata (for IdP configuration)
     *
     * GET /api/auth/sso/metadata/{enterpriseId}
     *
     * Returns Service Provider metadata XML for IdP setup
     *
     * @param enterpriseId Enterprise ID
     * @return SAML metadata XML
     */
    @GetMapping(value = "/metadata/{enterpriseId}", produces = "application/xml")
    public ResponseEntity<?> getSamlMetadata(@PathVariable UUID enterpriseId) {
        try {
            String metadataXml = ssoService.generateSamlMetadata(enterpriseId);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/xml")
                    .body(metadataXml);
        } catch (Exception e) {
            log.error("Generate SAML metadata failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate SAML metadata");
        }
    }

    /**
     * Test SSO configuration
     *
     * POST /api/auth/sso/test
     *
     * Requires: Authorization header with ENTERPRISE_ADMIN token
     *
     * @return Test results
     */
    @PostMapping("/test")
    public ResponseEntity<?> testSsoConfig(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {
        try {
            String token = authHeader.replace("Bearer ", "");
            UUID enterpriseId = ssoService.getEnterpriseIdFromToken(token);

            var testResults = ssoService.testSsoConfiguration(enterpriseId);

            return ResponseEntity.ok(testResults);
        } catch (Exception e) {
            log.error("SSO test failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Disable SSO for enterprise
     *
     * POST /api/auth/sso/disable
     *
     * Requires: Authorization header with ENTERPRISE_ADMIN token
     *
     * @return Success message
     */
    @PostMapping("/disable")
    public ResponseEntity<?> disableSso(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {
        try {
            String token = authHeader.replace("Bearer ", "");
            UUID enterpriseId = ssoService.getEnterpriseIdFromToken(token);
            UUID userId = ssoService.getUserIdFromToken(token);

            ssoService.disableSso(enterpriseId);

            // Log audit trail
            auditLogService.logAction(
                userId,
                "SSO_DISABLED",
                "sso_config",
                enterpriseId,
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                "WARNING"
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "SSO disabled successfully"
            ));
        } catch (Exception e) {
            log.error("Disable SSO failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Single Logout (SLO)
     *
     * POST /api/auth/sso/logout
     *
     * Logs out user from both WaterAccessOptimizer and IdP
     *
     * @return Logout URL or success message
     */
    @PostMapping("/logout")
    public ResponseEntity<?> ssoLogout(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {
        try {
            String token = authHeader.replace("Bearer ", "");
            UUID userId = ssoService.getUserIdFromToken(token);

            // Get SSO session info
            var ssoSession = ssoService.getSsoSession(userId);

            if (ssoSession != null) {
                // Generate SLO request
                String logoutUrl = ssoService.initiateSsoLogout(ssoSession);

                // Invalidate local session
                ssoService.invalidateSsoSession(userId);

                // Log audit trail
                auditLogService.logAction(
                    userId,
                    "SSO_LOGOUT",
                    "user",
                    userId,
                    getClientIp(httpRequest),
                    httpRequest.getHeader("User-Agent"),
                    "INFO"
                );

                return ResponseEntity.ok(Map.of(
                    "logoutUrl", logoutUrl,
                    "message", "SSO logout initiated"
                ));
            } else {
                // No SSO session, just local logout
                return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
            }
        } catch (Exception e) {
            log.error("SSO logout failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get SSO authentication logs (ENTERPRISE_ADMIN only)
     *
     * GET /api/auth/sso/logs
     *
     * @return SSO authentication history
     */
    @GetMapping("/logs")
    public ResponseEntity<?> getSsoLogs(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            String token = authHeader.replace("Bearer ", "");
            UUID enterpriseId = ssoService.getEnterpriseIdFromToken(token);

            var logs = ssoService.getSsoLogs(enterpriseId, page, size);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("Get SSO logs failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
