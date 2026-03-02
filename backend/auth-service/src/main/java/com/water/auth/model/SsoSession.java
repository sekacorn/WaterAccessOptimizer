package com.water.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SSO Session Model
 *
 * Tracks active SSO sessions for Single Logout (SLO)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SsoSession {

    private UUID id;
    private UUID userId;
    private UUID enterpriseId;
    private String provider;
    private String sessionIndex; // SAML SessionIndex
    private String nameId; // SAML NameID
    private String nameIdFormat;
    private String idpSessionId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String ipAddress;
    private String userAgent;
}
