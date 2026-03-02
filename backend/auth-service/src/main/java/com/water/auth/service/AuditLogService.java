package com.water.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit Log Service (MVP v0.1.0)
 *
 * Logs security and administrative actions for compliance
 */
@Service
@Slf4j
public class AuditLogService {

    /**
     * Log an authentication event (login, registration, etc.)
     */
    public void logAuthEvent(UUID userId, String eventType, String ipAddress,
                            String userAgent, boolean success, String errorMessage) {
        // In real implementation, save to audit_logs table
        String timestamp = LocalDateTime.now().toString();
        if (success) {
            log.info("AUTH_AUDIT: timestamp={}, userId={}, event={}, ip={}, userAgent={}, success={}",
                    timestamp, userId, eventType, ipAddress, userAgent, success);
        } else {
            log.warn("AUTH_AUDIT: timestamp={}, userId={}, event={}, ip={}, userAgent={}, success={}, error={}",
                    timestamp, userId, eventType, ipAddress, userAgent, success, errorMessage);
        }
    }

    /**
     * Log a general action
     */
    public void logAction(UUID userId, String action, String resourceType,
                         UUID resourceId, String ipAddress, String userAgent,
                         String severity) {
        // In real implementation, save to audit_logs table
        log.info("AUDIT: user={}, action={}, resource={}:{}, ip={}, severity={}",
                userId, action, resourceType, resourceId, ipAddress, severity);
    }
}
