package com.water.auth.controller;

import com.water.auth.dto.MfaSetupResponse;
import com.water.auth.dto.MfaVerifyRequest;
import com.water.auth.service.MfaService;
import com.water.auth.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Multi-Factor Authentication (MFA) Controller
 *
 * Handles MFA operations:
 * - Setup TOTP (Google Authenticator, Authy, etc.)
 * - Enable/Disable MFA
 * - Verify MFA codes
 * - Generate backup codes
 * - Manage trusted devices
 *
 * MFA is recommended for:
 * - All enterprise users
 * - Users with MODERATOR, ADMIN, ENTERPRISE_ADMIN, SUPER_ADMIN roles
 * - Users handling sensitive water data
 */
@RestController
@RequestMapping("/api/auth/mfa")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MfaController {

    private final MfaService mfaService;
    private final AuditLogService auditLogService;

    /**
     * Step 1: Setup MFA - Generate QR code for authenticator app
     *
     * POST /api/auth/mfa/setup
     *
     * Requires: Authorization header with JWT token
     *
     * @return QR code image URL, secret key, and backup codes
     */
    @PostMapping("/setup")
    public ResponseEntity<?> setupMfa(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "TOTP") String mfaType,
            HttpServletRequest httpRequest) {
        try {
            String token = authHeader.replace("Bearer ", "");
            UUID userId = mfaService.getUserIdFromToken(token);

            log.info("MFA setup initiated for user: {}", userId);

            // Generate TOTP secret and QR code
            MfaSetupResponse response = mfaService.setupMfa(userId, mfaType);

            // Log audit trail
            auditLogService.logAction(
                userId,
                "MFA_SETUP_INITIATED",
                "mfa",
                userId,
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                "INFO"
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("MFA setup failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Step 2: Verify and enable MFA
     *
     * POST /api/auth/mfa/enable
     *
     * Requires: Authorization header with JWT token
     *
     * @param request Verification code from authenticator app
     * @return Success message
     */
    @PostMapping("/enable")
    public ResponseEntity<?> enableMfa(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody MfaVerifyRequest request,
            HttpServletRequest httpRequest) {
        try {
            String token = authHeader.replace("Bearer ", "");
            UUID userId = mfaService.getUserIdFromToken(token);

            log.info("MFA enable attempt for user: {}", userId);

            // Verify code and enable MFA
            boolean success = mfaService.enableMfa(userId, request.getCode());

            if (success) {
                // Log audit trail
                auditLogService.logAction(
                    userId,
                    "MFA_ENABLED",
                    "mfa",
                    userId,
                    getClientIp(httpRequest),
                    httpRequest.getHeader("User-Agent"),
                    "INFO"
                );

                // Log MFA attempt
                mfaService.logMfaAttempt(
                    userId,
                    "setup",
                    true,
                    getClientIp(httpRequest),
                    httpRequest.getHeader("User-Agent")
                );

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "MFA enabled successfully",
                    "backupCodes", mfaService.getBackupCodes(userId)
                ));
            } else {
                // Log failed attempt
                mfaService.logMfaAttempt(
                    userId,
                    "setup",
                    false,
                    getClientIp(httpRequest),
                    httpRequest.getHeader("User-Agent")
                );

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid verification code"));
            }
        } catch (Exception e) {
            log.error("MFA enable failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verify MFA code during login
     *
     * POST /api/auth/mfa/verify
     *
     * This endpoint is called after successful username/password authentication
     *
     * @param request Verification code or backup code
     * @return Success status
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest request,
            HttpServletRequest httpRequest) {
        try {
            log.info("MFA verification attempt for user: {}", request.getUserId());

            boolean success = mfaService.verifyMfaCode(
                request.getUserId(),
                request.getCode(),
                request.isBackupCode()
            );

            // Log MFA attempt
            mfaService.logMfaAttempt(
                request.getUserId(),
                "login",
                success,
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
            );

            if (success) {
                // Check if user wants to trust this device
                if (request.isTrustDevice()) {
                    String deviceFingerprint = request.getDeviceFingerprint();
                    mfaService.trustDevice(
                        request.getUserId(),
                        deviceFingerprint,
                        getClientIp(httpRequest),
                        httpRequest.getHeader("User-Agent")
                    );
                }

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "MFA verification successful"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid verification code"));
            }
        } catch (Exception e) {
            log.error("MFA verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "MFA verification failed"));
        }
    }

    /**
     * Disable MFA for user account
     *
     * POST /api/auth/mfa/disable
     *
     * Requires: Authorization header with JWT token and current password
     *
     * @param password User's current password for security
     * @return Success message
     */
    @PostMapping("/disable")
    public ResponseEntity<?> disableMfa(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String password,
            HttpServletRequest httpRequest) {
        try {
            String token = authHeader.replace("Bearer ", "");
            UUID userId = mfaService.getUserIdFromToken(token);

            log.info("MFA disable attempt for user: {}", userId);

            boolean success = mfaService.disableMfa(userId, password);

            if (success) {
                // Log audit trail
                auditLogService.logAction(
                    userId,
                    "MFA_DISABLED",
                    "mfa",
                    userId,
                    getClientIp(httpRequest),
                    httpRequest.getHeader("User-Agent"),
                    "WARNING"
                );

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "MFA disabled successfully"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid password"));
            }
        } catch (Exception e) {
            log.error("MFA disable failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get MFA status for current user
     *
     * GET /api/auth/mfa/status
     *
     * Requires: Authorization header with JWT token
     *
     * @return MFA enabled status and configuration
     */
    @GetMapping("/status")
    public ResponseEntity<?> getMfaStatus(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            UUID userId = mfaService.getUserIdFromToken(token);

            Map<String, Object> status = mfaService.getMfaStatus(userId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Get MFA status failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Regenerate backup codes
     *
     * POST /api/auth/mfa/backup-codes/regenerate
     *
     * Requires: Authorization header with JWT token
     *
     * @return New backup codes
     */
    @PostMapping("/backup-codes/regenerate")
    public ResponseEntity<?> regenerateBackupCodes(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {
        try {
            String token = authHeader.replace("Bearer ", "");
            UUID userId = mfaService.getUserIdFromToken(token);

            log.info("Regenerating backup codes for user: {}", userId);

            String[] backupCodes = mfaService.regenerateBackupCodes(userId);

            // Log audit trail
            auditLogService.logAction(
                userId,
                "MFA_BACKUP_CODES_REGENERATED",
                "mfa",
                userId,
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                "INFO"
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "backupCodes", backupCodes
            ));
        } catch (Exception e) {
            log.error("Regenerate backup codes failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get trusted devices
     *
     * GET /api/auth/mfa/trusted-devices
     *
     * Requires: Authorization header with JWT token
     *
     * @return List of trusted devices
     */
    @GetMapping("/trusted-devices")
    public ResponseEntity<?> getTrustedDevices(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            UUID userId = mfaService.getUserIdFromToken(token);

            var devices = mfaService.getTrustedDevices(userId);
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            log.error("Get trusted devices failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Remove trusted device
     *
     * DELETE /api/auth/mfa/trusted-devices/{deviceId}
     *
     * Requires: Authorization header with JWT token
     *
     * @param deviceId Device ID to remove
     * @return Success message
     */
    @DeleteMapping("/trusted-devices/{deviceId}")
    public ResponseEntity<?> removeTrustedDevice(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID deviceId,
            HttpServletRequest httpRequest) {
        try {
            String token = authHeader.replace("Bearer ", "");
            UUID userId = mfaService.getUserIdFromToken(token);

            log.info("Removing trusted device {} for user: {}", deviceId, userId);

            mfaService.removeTrustedDevice(userId, deviceId);

            // Log audit trail
            auditLogService.logAction(
                userId,
                "MFA_TRUSTED_DEVICE_REMOVED",
                "device",
                deviceId,
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                "INFO"
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Trusted device removed"
            ));
        } catch (Exception e) {
            log.error("Remove trusted device failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check if device is trusted
     *
     * POST /api/auth/mfa/check-device
     *
     * @param deviceFingerprint Browser/device fingerprint
     * @return Whether device is trusted
     */
    @PostMapping("/check-device")
    public ResponseEntity<?> checkTrustedDevice(@RequestParam String deviceFingerprint) {
        try {
            boolean isTrusted = mfaService.isDeviceTrusted(deviceFingerprint);
            return ResponseEntity.ok(Map.of("trusted", isTrusted));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("trusted", false));
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
