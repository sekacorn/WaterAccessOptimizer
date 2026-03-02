package com.water.auth.controller;

import com.water.auth.dto.*;
import com.water.auth.service.AuthService;
import com.water.auth.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication Controller
 *
 * Handles all authentication-related endpoints:
 * - User registration and login
 * - Token generation and refresh
 * - Password reset
 * - Role and permission management
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final AuditLogService auditLogService;

    /**
     * Register a new user account (MVP v0.1.0)
     *
     * POST /api/auth/register
     *
     * @param request Registration details (email, password, firstName, lastName, organization)
     * @return JWT token and user information
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        try {
            // AuthService handles audit logging internally
            AuthResponse response = authService.register(request, httpRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * User login (MVP v0.1.0)
     *
     * POST /api/auth/login
     *
     * @param request Login credentials (email and password)
     * @return JWT token and user information
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            // AuthService handles audit logging internally
            AuthResponse response = authService.login(request, httpRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Refresh JWT token
     *
     * POST /api/auth/refresh
     *
     * @param request Refresh token
     * @return New JWT access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            String newToken = authService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(Map.of("token", newToken));
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid refresh token"));
        }
    }

    /**
     * Validate JWT token
     *
     * GET /api/auth/validate
     *
     * @param token JWT token
     * @return Token validity status
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        try {
            boolean isValid = authService.validateToken(token);
            return ResponseEntity.ok(Map.of("valid", isValid));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("valid", false));
        }
    }

    /**
     * Get current user information
     *
     * GET /api/auth/me
     *
     * Requires: Authorization header with JWT token
     *
     * @return Current user details
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            UserResponse user = authService.getCurrentUser(token);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Get current user failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }
    }

    /**
     * Request password reset
     *
     * POST /api/auth/forgot-password
     *
     * @param request Email address for password reset
     * @return Success message
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.initiatePasswordReset(request.getEmail());
            return ResponseEntity.ok(Map.of(
                "message", "If the email exists, a password reset link has been sent"
            ));
        } catch (Exception e) {
            // Always return success to prevent email enumeration
            return ResponseEntity.ok(Map.of(
                "message", "If the email exists, a password reset link has been sent"
            ));
        }
    }

    /**
     * Reset password with token
     *
     * POST /api/auth/reset-password
     *
     * @param request Password reset token and new password
     * @return Success message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password reset successful"));
        } catch (Exception e) {
            log.error("Password reset failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid or expired reset token"));
        }
    }

    /**
     * Change password for authenticated user
     *
     * POST /api/auth/change-password
     *
     * Requires: Authorization header with JWT token
     *
     * @param request Current and new password
     * @return Success message
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        try {
            String token = authHeader.replace("Bearer ", "");
            authService.changePassword(token, request.getCurrentPassword(), request.getNewPassword());

            // Log audit trail
            UserResponse user = authService.getCurrentUser(token);
            auditLogService.logAction(
                user.getId(),
                "PASSWORD_CHANGE",
                "user",
                user.getId(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                "INFO"
            );

            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            log.error("Password change failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "auth-service"
        ));
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
