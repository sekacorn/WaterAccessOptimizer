package com.water.auth.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

/**
 * Service for tracking custom auth-specific metrics.
 */
@Service
public class AuthMetricsService {

    private final MeterRegistry meterRegistry;

    // Counters
    private Counter loginSuccessCounter;
    private Counter loginFailureCounter;
    private Counter registrationCounter;
    private Counter tokenRefreshCounter;
    private Counter logoutCounter;
    private Counter mfaEnabledCounter;
    private Counter mfaVerificationCounter;

    // Timers
    private Timer loginTimer;
    private Timer registrationTimer;

    public AuthMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        // Initialize counters
        loginSuccessCounter = Counter.builder("water.auth.login.success")
                .description("Number of successful logins")
                .tag("service", "auth")
                .register(meterRegistry);

        loginFailureCounter = Counter.builder("water.auth.login.failure")
                .description("Number of failed login attempts")
                .tag("service", "auth")
                .register(meterRegistry);

        registrationCounter = Counter.builder("water.auth.registration.total")
                .description("Number of user registrations")
                .tag("service", "auth")
                .register(meterRegistry);

        tokenRefreshCounter = Counter.builder("water.auth.token.refresh")
                .description("Number of token refreshes")
                .tag("service", "auth")
                .register(meterRegistry);

        logoutCounter = Counter.builder("water.auth.logout.total")
                .description("Number of user logouts")
                .tag("service", "auth")
                .register(meterRegistry);

        mfaEnabledCounter = Counter.builder("water.auth.mfa.enabled")
                .description("Number of MFA activations")
                .tag("service", "auth")
                .register(meterRegistry);

        mfaVerificationCounter = Counter.builder("water.auth.mfa.verification")
                .description("Number of MFA verifications")
                .tag("service", "auth")
                .register(meterRegistry);

        // Initialize timers
        loginTimer = Timer.builder("water.auth.login.duration")
                .description("Login request duration")
                .tag("service", "auth")
                .register(meterRegistry);

        registrationTimer = Timer.builder("water.auth.registration.duration")
                .description("Registration request duration")
                .tag("service", "auth")
                .register(meterRegistry);
    }

    // Counter methods
    public void incrementLoginSuccess() {
        loginSuccessCounter.increment();
    }

    public void incrementLoginFailure() {
        loginFailureCounter.increment();
    }

    public void incrementRegistration() {
        registrationCounter.increment();
    }

    public void incrementTokenRefresh() {
        tokenRefreshCounter.increment();
    }

    public void incrementLogout() {
        logoutCounter.increment();
    }

    public void incrementMfaEnabled() {
        mfaEnabledCounter.increment();
    }

    public void incrementMfaVerification() {
        mfaVerificationCounter.increment();
    }

    // Timer methods
    public Timer.Sample startLoginTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordLoginTime(Timer.Sample sample) {
        sample.stop(loginTimer);
    }

    public Timer.Sample startRegistrationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordRegistrationTime(Timer.Sample sample) {
        sample.stop(registrationTimer);
    }
}
