# Security Implementation TODO - MVP

**Document Purpose**: Track remaining security implementation tasks for MVP v0.1.0 based on Agent 16 (Security/IAM Architecture)

**Last Updated**: 2026-02-02 (Iteration 3)

---

## Completed (Iteration 3)

### [X]JWT Authentication (Basic)
- JWT validation in API Gateway with signature verification
- Header propagation to downstream services (X-User-Id, X-User-Email, X-User-Role, X-Request-Id)
- Public endpoint handling (no auth required for /api/auth/register, /api/auth/login)
- Token type claim ("access") added for future refresh token support
- 24-hour token expiry (will be reduced to 15 min when refresh tokens added)

### [X]Audit Logging (Basic)
- AuditLogService exists in auth-service
- Logs authentication events to slf4j
- **Note**: Not yet persisted to database (logs to console only)

---

## TODO: Sprint 2 (Weeks 3-4) - Refresh Tokens

### Priority 1: Refresh Token Implementation

**Database Schema** (Add to auth_schema):
```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_jti VARCHAR(255) UNIQUE NOT NULL,  -- JWT ID from token
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Device fingerprinting
    ip_address INET,
    user_agent TEXT,
    device_fingerprint VARCHAR(64),

    CONSTRAINT check_not_expired CHECK (revoked_at IS NULL OR revoked_at <= NOW())
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_jti ON refresh_tokens(token_jti);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

**Code Changes Required**:

1. **backend/auth-service/src/main/java/com/water/auth/model/RefreshToken.java** (NEW)
   ```java
   @Entity
   @Table(name = "refresh_tokens", schema = "auth_schema")
   public class RefreshToken {
       @Id
       private UUID id;

       @Column(name = "user_id", nullable = false)
       private UUID userId;

       @Column(name = "token_jti", nullable = false, unique = true)
       private String tokenJti;

       @Column(name = "expires_at", nullable = false)
       private LocalDateTime expiresAt;

       @Column(name = "revoked_at")
       private LocalDateTime revokedAt;

       @Column(name = "ip_address")
       private String ipAddress;

       @Column(name = "user_agent")
       private String userAgent;

       @Column(name = "device_fingerprint")
       private String deviceFingerprint;

       // getters, setters, constructors
   }
   ```

2. **backend/auth-service/src/main/java/com/water/auth/repository/RefreshTokenRepository.java** (NEW)
   ```java
   public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
       Optional<RefreshToken> findByTokenJti(String tokenJti);
       List<RefreshToken> findByUserId(UUID userId);
       int deleteByExpiresAtBefore(LocalDateTime expiresAt);
   }
   ```

3. **Update backend/auth-service/src/main/java/com/water/auth/service/JwtService.java**
   - Add `generateRefreshToken(User user, String tokenId)` method
   - Reduce access token expiry from 24 hours to 15 minutes
   - Add token type validation (`validateToken` should check token type)

4. **backend/auth-service/src/main/java/com/water/auth/service/RefreshTokenService.java** (NEW)
   - `createRefreshToken(User user, HttpServletRequest request)` - Generate and store
   - `revokeRefreshToken(String tokenJti)` - Mark as revoked
   - `isRefreshTokenValid(String tokenJti)` - Check not revoked and not expired
   - `rotateRefreshToken(String oldTokenJti)` - Revoke old, issue new (security best practice)
   - `generateDeviceFingerprint(HttpServletRequest request)` - SHA-256 hash of user agent + other info

5. **Update backend/auth-service/src/main/java/com/water/auth/controller/AuthController.java**
   - Modify `login()` to return both access_token AND refresh_token
   - Add `POST /auth/refresh` endpoint to exchange refresh token for new access token
   - Add `POST /auth/logout` endpoint to revoke refresh token

6. **Scheduled Job for Cleanup**
   ```java
   @Scheduled(cron = "0 0 2 * * *")  // Daily at 2 AM
   public void cleanupExpiredTokens() {
       int deleted = refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
       log.info("Deleted {} expired refresh tokens", deleted);
   }
   ```

7. **Frontend Changes**
   - Store refresh_token in httpOnly cookie (secure, sameSite=strict)
   - Store access_token in memory (React state, not localStorage)
   - Implement token refresh interceptor (auto-refresh on 401 Unauthorized)
   - Update api.js axios interceptor

**Testing**:
- Verify access token expires after 15 minutes
- Verify refresh token works to get new access token
- Verify refresh token rotation (old token revoked, new token issued)
- Verify logout revokes refresh token
- Verify expired refresh tokens cannot be used

---

## TODO: Sprint 3-4 (Weeks 5-6) - Enhanced Security

### Priority 2: Account Lockout

**Database Schema** (Add columns to users table):
```sql
ALTER TABLE auth_schema.users
ADD COLUMN failed_login_attempts INTEGER DEFAULT 0,
ADD COLUMN locked_until TIMESTAMP;
```

**Code Changes**:
1. **Update backend/auth-service/src/main/java/com/water/auth/model/User.java**
   - Add `failedLoginAttempts` field
   - Add `lockedUntil` field

2. **Update backend/auth-service/src/main/java/com/water/auth/service/AuthService.java**
   - In `login()` method:
     - Check if `lockedUntil` > NOW() → throw AccountLockedException
     - On password failure: increment `failedLoginAttempts`
     - If `failedLoginAttempts` >= 5: set `lockedUntil` = NOW() + 30 minutes
     - On success: reset `failedLoginAttempts` = 0, `lockedUntil` = null
   - Add `AccountLockedException` class

**Testing**:
- Try 5 failed logins, verify account locked
- Verify locked account returns 429 with "locked until" message
- Verify successful login after 30 minutes clears lockout

---

### Priority 3: Database-Backed Audit Logging

**Current State**: AuditLogService logs to slf4j console only

**Database Schema** (Already in Agent 03 schema):
```sql
CREATE TABLE auth_schema.audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    event_type VARCHAR(100) NOT NULL,
    event_category VARCHAR(50),
    resource_type VARCHAR(50),
    resource_id UUID,
    ip_address INET,
    user_agent TEXT,
    http_method VARCHAR(10),
    endpoint VARCHAR(255),
    status_code INTEGER,
    details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_logs_event_type ON audit_logs(event_type, created_at DESC);
CREATE INDEX idx_audit_logs_category ON audit_logs(event_category, created_at DESC);
```

**Code Changes**:
1. **backend/auth-service/src/main/java/com/water/auth/model/AuditLog.java** (NEW)
   - JPA entity mapping to audit_logs table

2. **backend/auth-service/src/main/java/com/water/auth/repository/AuditLogRepository.java** (NEW)
   ```java
   public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
       List<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
       List<AuditLog> findByEventCategoryOrderByCreatedAtDesc(String eventCategory, Pageable pageable);
   }
   ```

3. **Update backend/auth-service/src/main/java/com/water/auth/service/AuditLogService.java**
   - Replace `log.info()` with `auditLogRepository.save()`
   - Extract IP address and user agent from request context
   - Add `details` JSONB field for structured event data

4. **Add Admin Endpoint**
   - `GET /admin/audit-logs?userId={uuid}&page=0&limit=100`
   - `GET /admin/audit-logs?eventCategory=auth&page=0&limit=100`

**Events to Log** (MVP subset from Agent 16):
- LOGIN_SUCCESS, LOGIN_FAILED
- LOGOUT
- ACCOUNT_LOCKED
- PASSWORD_CHANGED
- USER_CREATED (admin action)
- USER_ROLE_CHANGED (admin action)
- USER_DEACTIVATED (admin action)
- DATA_UPLOADED, DATA_DELETED
- ASSESSMENT_CREATED, ASSESSMENT_DELETED
- PERMISSION_DENIED

**Retention Policy**:
- Keep all audit logs for 90 days (minimum)
- Archive older logs to S3/object storage (V2 feature)

**Testing**:
- Trigger login, verify audit_log entry created
- Check admin can query audit logs by user or event category
- Verify JSONB details field contains structured data

---

### Priority 4: Rate Limiting (Redis-Based)

**Current State**: No rate limiting implemented

**Dependencies**:
- Redis (already in docker-compose.yml)
- Spring Data Redis

**Code Changes**:

1. **backend/api-gateway/pom.xml** - Add dependencies:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
   </dependency>
   ```

2. **backend/api-gateway/src/main/java/com/water/gateway/filter/RateLimitFilter.java** (NEW)
   ```java
   @Component
   public class RateLimitFilter implements GlobalFilter, Ordered {

       @Autowired
       private ReactiveRedisTemplate<String, String> redisTemplate;

       @Override
       public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
           String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
           String path = exchange.getRequest().getURI().getPath();

           // Determine limit based on endpoint
           RateLimitConfig config = getRateLimitConfig(path);
           String key = "ratelimit:" + userId + ":" + config.getAction();

           return redisTemplate.opsForValue().increment(key)
               .flatMap(count -> {
                   if (count == 1) {
                       redisTemplate.expire(key, Duration.ofSeconds(config.getWindowSeconds()));
                   }

                   if (count > config.getLimit()) {
                       exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                       exchange.getResponse().getHeaders().add("Retry-After",
                           String.valueOf(config.getWindowSeconds()));
                       return exchange.getResponse().setComplete();
                   }

                   exchange.getResponse().getHeaders().add("X-RateLimit-Limit",
                       String.valueOf(config.getLimit()));
                   exchange.getResponse().getHeaders().add("X-RateLimit-Remaining",
                       String.valueOf(config.getLimit() - count));

                   return chain.filter(exchange);
               });
       }
   }
   ```

3. **Rate Limit Configuration** (application.yml):
   ```yaml
   ratelimit:
     default: 100  # requests per minute
     limits:
       - path: /api/data/upload/*
         limit: 10
         window: 3600  # per hour
       - path: /api/analysis/*
         limit: 20
         window: 3600  # per hour
   ```

**Testing**:
- Send 101 requests in 60 seconds, verify 101st returns 429
- Verify X-RateLimit-Limit and X-RateLimit-Remaining headers
- Verify rate limit resets after window expires

---

### Priority 5: Input Validation

**Code Changes**:

1. **backend/auth-service/src/main/java/com/water/auth/dto/RegisterRequest.java**
   - Add JSR-303 validation annotations:
   ```java
   @NotBlank(message = "Email is required")
   @Email(message = "Invalid email format")
   @Size(max = 255, message = "Email too long")
   private String email;

   @NotBlank(message = "Password is required")
   @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "Password must be 8+ chars with uppercase, lowercase, number")
   private String password;
   ```

2. **backend/data-service** (when created)
   - File size validation (max 10MB)
   - File type validation (CSV, JSON, GeoJSON only)
   - Filename validation (no path traversal: `..`, `/`, `\`)
   - Coordinate validation (lat -90 to 90, lon -180 to 180)

**Testing**:
- Try weak password "password", verify rejected
- Try invalid email "notanemail", verify rejected
- Try upload 11MB file, verify 413 Payload Too Large

---

## TODO: Sprint 5-6 (Weeks 7-8) - Production Hardening

### Priority 6: Security Headers (NGINX)

**File**: `infra/nginx/nginx.conf` or docker-compose NGINX service

**Headers to Add**:
```nginx
# Security headers
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "DENY" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:;" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;
```

**Testing**:
- Use https://securityheaders.com/ to scan site
- Verify A+ rating

---

### Priority 7: CORS Configuration

**Code Changes**:

1. **backend/api-gateway/src/main/resources/application.yml**
   ```yaml
   spring:
     cloud:
       gateway:
         globalcors:
           corsConfigurations:
             '[/**]':
               allowedOrigins:
                 - "https://wateroptimizer.org"
                 - "http://localhost:3000"  # Dev only
               allowedMethods:
                 - GET
                 - POST
                 - PUT
                 - DELETE
                 - PATCH
               allowedHeaders: "*"
               allowCredentials: true
               maxAge: 3600
   ```

**Testing**:
- Verify frontend (localhost:3000) can make API calls
- Verify other origins blocked (403 CORS error)

---

### Priority 8: Error Handling (Hide Stack Traces)

**Code Changes**:

1. **backend/*/src/main/resources/application-production.yml** (all services)
   ```yaml
   server:
     error:
       include-message: false
       include-stacktrace: never
       include-binding-errors: never
   ```

2. **Global Exception Handler**
   ```java
   @ControllerAdvice
   public class GlobalExceptionHandler {

       @ExceptionHandler(Exception.class)
       public ResponseEntity<ErrorResponse> handleException(Exception e) {
           log.error("Unexpected error", e);  // Log full stack trace

           // Return generic error to user (no stack trace)
           return ResponseEntity.status(500).body(
               new ErrorResponse("Internal server error", "ERR_INTERNAL")
           );
       }
   }
   ```

**Testing**:
- Trigger error (divide by zero, null pointer, etc.)
- Verify response doesn't include stack trace
- Verify logs still contain full stack trace

---

### Priority 9: Dependency Scanning

**GitHub Actions** (`.github/workflows/security.yml`):
```yaml
name: Security Scan

on: [push, pull_request]

jobs:
  dependency-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'

      - name: Run OWASP Dependency Check
        run: mvn dependency-check:check

      - name: Upload report
        uses: actions/upload-artifact@v3
        with:
          name: dependency-check-report
          path: target/dependency-check-report.html

  npm-audit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'

      - name: Run npm audit
        run: |
          cd frontend
          npm audit --audit-level=high
```

**Testing**:
- Run `mvn dependency-check:check` locally
- Run `npm audit` in frontend
- Verify no HIGH or CRITICAL vulnerabilities

---

### Priority 10: Security Testing

**OWASP ZAP Scan**:
```bash
docker run -t owasp/zap2docker-stable zap-baseline.py \
  -t http://localhost:8080 \
  -r zap_report.html
```

**Manual Testing Checklist**:
- [ ] Try SQL injection: `' OR '1'='1`
- [ ] Try XSS: `<script>alert('xss')</script>`
- [ ] Try path traversal: `../../etc/passwd`
- [ ] Try accessing another user's dataset
- [ ] Try changing own role to ADMIN
- [ ] Verify passwords hashed in database (bcrypt, starts with `$2a$` or `$2b$`)
- [ ] Verify HTTPS redirect works
- [ ] Verify CORS blocks unauthorized origins

---

## V2 Features (Not in MVP)

These are documented in Agent 16 but NOT required for MVP v0.1.0:

- MFA (TOTP) with backup codes and trusted devices (V2, Weeks 13-16)
- SSO (SAML 2.0, OIDC) with enterprise tenant management (V2, Weeks 17-20)
- Enhanced rate limiting with per-IP limits (V2)
- Penetration testing (V2, before production launch)
- SOC 2 Type II compliance (V2)

---

## Summary

**Completed (Iteration 3)**:
- JWT validation in API Gateway [X]
- Header propagation (X-User-Id, X-User-Email, X-User-Role, X-Request-Id) [X]
- Token type claim ("access") [X]
- Basic audit logging to slf4j [X]

**TODO for MVP (Sprints 2-6)**:
1. Refresh tokens with rotation (Sprint 2) - HIGH PRIORITY
2. Account lockout after 5 failed logins (Sprint 3) - HIGH PRIORITY
3. Database-backed audit logging (Sprint 3) - MEDIUM PRIORITY
4. Redis-based rate limiting (Sprint 4) - MEDIUM PRIORITY
5. Input validation with JSR-303 (Sprint 4) - MEDIUM PRIORITY
6. Security headers in NGINX (Sprint 5) - LOW PRIORITY
7. CORS configuration (Sprint 5) - LOW PRIORITY
8. Error handling (hide stack traces) (Sprint 5) - LOW PRIORITY
9. Dependency scanning (Sprint 6) - LOW PRIORITY
10. Security testing with OWASP ZAP (Sprint 6) - LOW PRIORITY

**Estimated Effort**:
- Refresh tokens: 3-4 days
- Account lockout: 1 day
- Database audit logging: 2 days
- Rate limiting: 2 days
- Input validation: 1-2 days
- Production hardening (items 6-10): 2-3 days

**Total**: ~12-15 days (fits within 6 sprints)

---

**Document Status**: [X]Complete
**Last Updated**: 2026-02-02 (Iteration 3)
**Next Review**: After Sprint 2 completion
