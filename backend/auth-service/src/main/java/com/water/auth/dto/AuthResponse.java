package com.water.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Authentication Response DTO (MVP v0.1.0)
 *
 * Returned after successful registration or login
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * JWT Access Token (24-hour expiration)
     */
    private String token;

    /**
     * JWT refresh token (7-day expiration)
     */
    private String refreshToken;

    /**
     * User information (safe subset, no password hash)
     */
    private UserDto user;

    /**
     * Nested UserDto to avoid exposing sensitive fields
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private UUID id;
        private String email;
        private String firstName;
        private String lastName;
        private String organization;
        private String role;
        private Integer storageQuotaMb;
        private LocalDateTime createdAt;
        private LocalDateTime lastLogin;
    }
}
