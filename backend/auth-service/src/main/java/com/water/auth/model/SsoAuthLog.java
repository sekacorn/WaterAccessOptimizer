package com.water.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SSO Authentication Log Model
 *
 * Tracks all SSO authentication attempts for audit and compliance
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SsoAuthLog {

    private UUID id;
    private UUID enterpriseId;
    private UUID userId;
    private String email;
    private String provider;
    private String action; // login, logout
    private Boolean success;
    private String errorMessage;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp;
}
