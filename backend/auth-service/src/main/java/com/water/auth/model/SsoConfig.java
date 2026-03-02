package com.water.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SSO Configuration Model
 *
 * Stores SSO configuration for enterprise accounts
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SsoConfig {

    private UUID id;
    private UUID enterpriseId;
    private String provider;
    private String protocol; // SAML2, OIDC
    private String idpEntityId;
    private String idpSsoUrl;
    private String idpCertificate;
    private String clientId;
    private String clientSecret; // Encrypted
    private List<String> scopes;
    private List<String> allowedDomains;
    private Boolean forceSso;
    private Boolean autoProvision;
    private String defaultRole;
    private Map<String, String> attributeMappings;
    private String sloUrl;
    private Boolean jitProvisioning;
    private Integer sessionTimeoutMinutes;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
