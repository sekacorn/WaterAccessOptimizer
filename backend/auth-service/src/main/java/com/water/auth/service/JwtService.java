package com.water.auth.service;

import com.water.auth.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Service (MVP v0.1.0)
 *
 * Handles JWT token generation and validation
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 hours default (MVP)
    private Long jwtExpiration;

    /**
     * Generate JWT access token for user
     *
     * Token structure (MVP v0.1.0):
     * - sub: User ID (UUID)
     * - email: User email address
     * - role: User role (USER or ADMIN)
     * - type: "access" (distinguishes from future refresh tokens)
     * - iat: Issued at timestamp
     * - exp: Expiration timestamp (24 hours in MVP, will be 15 min in V1 with refresh tokens)
     *
     * TODO (Sprint 2): Add refresh token support with 7-day expiry and rotation
     */
    public String generateToken(User user) {
        return generateAccessToken(user);
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole());
        claims.put("email", user.getEmail());
        claims.put("type", "access");  // Token type for future refresh token support

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getId().toString())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String generateRefreshToken(User user, String tokenId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("email", user.getEmail());

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getId().toString())
            .setId(tokenId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 604800000L))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean validateToken(String token) {
        parseClaims(token);
        return true;
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(parseClaims(token).get("type", String.class));
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
