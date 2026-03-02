package com.water.auth.service;

import com.water.auth.dto.AuthResponse;
import com.water.auth.dto.LoginRequest;
import com.water.auth.dto.RegisterRequest;
import com.water.auth.exception.AccountLockedException;
import com.water.auth.exception.DuplicateEmailException;
import com.water.auth.exception.InvalidCredentialsException;
import com.water.auth.exception.InvalidPasswordException;
import com.water.auth.model.User;
import com.water.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Authentication service handling user registration, login, and security policies (MVP v0.1.0).
 *
 * Security Features:
 * - bcrypt password hashing (cost factor 12)
 * - Account lockout after 5 failed login attempts (30-minute cooldown)
 * - Password validation (min 8 chars, 1 uppercase, 1 lowercase, 1 number)
 * - Audit logging for all authentication events
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    /**
     * Register a new user account
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        log.info("Registration attempt for email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email already exists - {}", request.getEmail());
            throw new DuplicateEmailException("Email already registered");
        }

        // Validate password strength
        List<String> passwordErrors = validatePassword(request.getPassword());
        if (!passwordErrors.isEmpty()) {
            log.warn("Registration failed: weak password for email {}", request.getEmail());
            throw new InvalidPasswordException(String.join("; ", passwordErrors));
        }

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setOrganization(request.getOrganization());
        user.setRole("USER"); // Default role
        user.setIsActive(true);
        user.setStorageQuotaMb(100); // Default quota

        user = userRepository.save(user);
        log.info("User registered successfully: {} (id={})", user.getEmail(), user.getId());

        // Generate JWT token
        String token = jwtService.generateToken(user);

        // Log successful registration
        auditLogService.logAuthEvent(
            user.getId(),
            "REGISTRATION_SUCCESS",
            getClientIp(httpRequest),
            httpRequest.getHeader("User-Agent"),
            true,
            null
        );

        return buildAuthResponse(token, user);
    }

    /**
     * Authenticate user and generate JWT token
     */
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> {
                log.warn("Login failed: email not found - {}", request.getEmail());
                auditLogService.logAuthEvent(
                    null,
                    "LOGIN_FAILED_EMAIL_NOT_FOUND",
                    getClientIp(httpRequest),
                    httpRequest.getHeader("User-Agent"),
                    false,
                    "Email not found"
                );
                return new InvalidCredentialsException("Invalid email or password");
            });

        // Check if account is locked
        if (user.isAccountLocked()) {
            log.warn("Login failed: account locked - {} (locked until {})",
                user.getEmail(), user.getLockedUntil());
            auditLogService.logAuthEvent(
                user.getId(),
                "LOGIN_FAILED_ACCOUNT_LOCKED",
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                false,
                "Account locked until " + user.getLockedUntil()
            );
            throw new AccountLockedException(
                "Account locked due to multiple failed login attempts. Try again in " +
                    getMinutesUntilUnlock(user) + " minutes."
            );
        }

        // Check if account is deactivated
        if (!user.getIsActive()) {
            log.warn("Login failed: account deactivated - {}", user.getEmail());
            auditLogService.logAuthEvent(
                user.getId(),
                "LOGIN_FAILED_ACCOUNT_DEACTIVATED",
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                false,
                "Account deactivated"
            );
            throw new InvalidCredentialsException("Account has been deactivated");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: invalid password - {}", user.getEmail());

            // Increment failed login attempts
            user.incrementFailedLoginAttempts();
            userRepository.save(user);

            auditLogService.logAuthEvent(
                user.getId(),
                "LOGIN_FAILED_INVALID_PASSWORD",
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"),
                false,
                "Failed attempts: " + user.getFailedLoginAttempts()
            );

            if (user.isAccountLocked()) {
                throw new AccountLockedException(
                    "Account locked due to multiple failed login attempts. Try again in 30 minutes."
                );
            }

            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Successful login - reset failed attempts
        user.resetFailedLoginAttempts();
        user.setLastLogin(LocalDateTime.now());
        user.setLoginCount(user.getLoginCount() + 1);
        userRepository.save(user);

        log.info("Login successful: {} (id={})", user.getEmail(), user.getId());

        // Generate JWT token
        String token = jwtService.generateToken(user);

        // Log successful login
        auditLogService.logAuthEvent(
            user.getId(),
            "LOGIN_SUCCESS",
            getClientIp(httpRequest),
            httpRequest.getHeader("User-Agent"),
            true,
            null
        );

        return buildAuthResponse(token, user);
    }

    /**
     * Validate password strength
     * Requirements: min 8 chars, 1 uppercase, 1 lowercase, 1 number
     */
    private List<String> validatePassword(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.length() < 8) {
            errors.add("Password must be at least 8 characters long");
        }
        if (password != null) {
            if (!password.chars().anyMatch(Character::isUpperCase)) {
                errors.add("Password must contain at least one uppercase letter");
            }
            if (!password.chars().anyMatch(Character::isLowerCase)) {
                errors.add("Password must contain at least one lowercase letter");
            }
            if (!password.chars().anyMatch(Character::isDigit)) {
                errors.add("Password must contain at least one number");
            }
        }

        return errors;
    }

    /**
     * Build authentication response DTO
     */
    private AuthResponse buildAuthResponse(String token, User user) {
        AuthResponse.UserDto userDto = new AuthResponse.UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setOrganization(user.getOrganization());
        userDto.setRole(user.getRole());
        userDto.setStorageQuotaMb(user.getStorageQuotaMb());
        userDto.setCreatedAt(user.getCreatedAt());
        userDto.setLastLogin(user.getLastLogin());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(userDto);

        return response;
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Calculate minutes until account unlock
     */
    private long getMinutesUntilUnlock(User user) {
        if (user.getLockedUntil() == null) {
            return 0;
        }
        long minutes = java.time.Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes();
        return Math.max(0, minutes);
    }
}
