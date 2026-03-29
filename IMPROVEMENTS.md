# WaterAccessOptimizer — Improvement Tasks for Codex

This document lists concrete, actionable improvements identified from a full codebase review. Tasks are organized by priority. Each task includes the exact file(s) to change and what to do.

---

## Priority 1 — Critical Security Fixes

### 1.1 Remove hardcoded fallback credentials from application.yml

**Files:**
- `backend/auth-service/src/main/resources/application.yml`
- `backend/data-service/src/main/resources/application.yml`
- `backend/api-gateway/src/main/resources/application.yml`

**Problem:** All three `application.yml` files use hardcoded fallback values for database credentials and JWT secrets:
```yaml
username: ${SPRING_DATASOURCE_USERNAME:wateradmin}
password: ${SPRING_DATASOURCE_PASSWORD:waterpass123}
jwt.secret: ${JWT_SECRET:your-secret-key-min-32-characters-change-in-production}
```

**Fix:** Remove the fallback values so the application **fails fast** if the environment variable is not set. Example:
```yaml
username: ${SPRING_DATASOURCE_USERNAME}
password: ${SPRING_DATASOURCE_PASSWORD}
jwt.secret: ${JWT_SECRET}
```

Add startup validation to print a clear error message if required secrets are missing.

---

### 1.2 Fix overly permissive CORS (`*` wildcard)

**Files:**
- `backend/api-gateway/src/main/java/com/water/gateway/config/GatewayConfig.java` — line with `corsConfig.setAllowedOrigins(Arrays.asList("*"))`
- `backend/api-gateway/src/main/resources/application.yml` — line with `allowedOrigins: "*"`
- `backend/auth-service/src/main/java/com/water/auth/controller/AuthController.java` — `@CrossOrigin(origins = "*")`
- `ai-model/water_predictor.py` — `allow_origins=["*"]`

**Problem:** Every service allows requests from any origin, defeating CORS protection.

**Fix:**
- Replace `"*"` with `${CORS_ALLOWED_ORIGINS:http://localhost:5173}` so it is environment-configurable.
- In `GatewayConfig.java`, use `corsConfig.setAllowedOriginPatterns(List.of(allowedOrigins))` where `allowedOrigins` is read from config.
- Remove `@CrossOrigin(origins = "*")` from `AuthController` — rely on the gateway's CORS config instead.

---

### 1.3 Add rate limiting to authentication endpoints

**File:** `backend/auth-service/src/main/java/com/water/auth/controller/AuthController.java` and Spring Security config.

**Problem:** No rate limiting on `/api/auth/login` or `/api/auth/register`. The account lockout (5 attempts) only protects individual accounts, not the system from mass brute-force or credential stuffing.

**Fix:** Add Spring's `bucket4j` or a `RateLimitingFilter` that limits login attempts per IP to 10 per minute. Example dependency to add to `pom.xml`:
```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>
```
Create a `RateLimitFilter` bean that blocks requests exceeding the threshold with HTTP 429.

---

### 1.4 Fix password reset tokens — enforce expiry and single-use

**File:** `backend/auth-service/src/main/java/com/water/auth/controller/AuthController.java` (lines 172–181 approx.)

**Problem:** Password reset tokens are accepted but there is no check that they expire or are marked used after first consumption.

**Fix:**
- Store reset tokens in the `users` table (or a separate `password_reset_tokens` table) with a `expires_at` timestamp and a `used` boolean.
- In the reset handler, verify `token.expiresAt.isAfter(Instant.now())` and `!token.used`.
- Mark `token.used = true` and save immediately after successful reset.

---

## Priority 2 — Architecture & Naming

### 2.1 Rename `backend/api-gateway` directory to `backend/data-service`

**Problem:** The README itself notes (line 228): *"api-gateway… (misnamed, functions as data-service)"*. The directory hosts the data upload, risk assessment, and map services — nothing to do with API gateway routing.

**Fix:**
1. Rename the directory from `backend/api-gateway` to `backend/data-service`.
2. Update all references in:
   - `docker-compose.prod.yml` — build context path
   - `docker-compose.dev.yml` — build context path
   - `.github/workflows/ci-cd.yml` — build steps referencing `backend/api-gateway`
   - `README.md` — directory structure section
   - `GETTING_STARTED.md` and `DEPLOYMENT.md` — any `cd backend/api-gateway` commands
3. Update the artifact ID in `backend/api-gateway/pom.xml` from whatever it currently is to `data-service`.

---

### 2.2 Fix license inconsistency — README says MIT, LICENSE file is Apache 2.0

**Files:** `README.md` (lines 9, 917, 1028), `LICENSE`

**Problem:** The README badge and copyright section declare MIT License, but the `LICENSE` file contains the Apache 2.0 license text. This is a legal ambiguity.

**Fix:** Decide which license to use, then make them consistent:
- If Apache 2.0: Update README badge to `Apache-2.0`, update line 917 to say "Apache 2.0 License", update line 1028 accordingly.
- If MIT: Replace the `LICENSE` file content with the MIT license text.

The `LICENSE` file was last committed as Apache 2.0, so Apache 2.0 is likely the intended license.

---

### 2.3 Add the AI model service to Docker Compose

**File:** `docker-compose.prod.yml`

**Problem:** The `ai-model/` directory contains a `Dockerfile` and a full FastAPI service (`water_predictor.py`), but it is not included in `docker-compose.prod.yml`. The README troubleshooting section mentions checking `http://localhost:8000/health` for AI predictions, but that service is never started by Compose.

**Fix:** Add the `ai-model` service to `docker-compose.prod.yml`:
```yaml
  ai-model:
    build:
      context: ./ai-model
      dockerfile: Dockerfile
    ports:
      - "8000:8000"
    environment:
      - MODEL_HOST=0.0.0.0
    depends_on:
      - postgres
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

---

## Priority 3 — Code Quality

### 3.1 Remove TODO comment and implement JWT refresh tokens

**File:** `backend/auth-service/src/main/java/com/water/auth/service/JwtService.java`

**Problem:** Line ~38 has `// TODO (Sprint 2): Add refresh token support with 7-day expiry and rotation`. Currently users are hard-logged out every 24 hours.

**Fix:** Implement a `generateRefreshToken(String username)` method that:
- Creates a JWT with `subject = username`, `expiry = 7 days`, and a `type = "refresh"` claim.
- In `AuthController`, return both `accessToken` and `refreshToken` on login.
- Add a `POST /api/auth/refresh` endpoint that validates the refresh token and issues a new access token.
- Store refresh token JTI (JWT ID) in Redis or the database to enable revocation.

---

### 3.2 Replace manual JSON string building with proper serialization

**File:** `backend/data-service/src/main/java/com/water/data/service/RiskScoringService.java` (lines ~378–391)

**Problem:** Risk explanation is built by concatenating strings with `String.format(...)` to produce JSON. This is brittle and could produce malformed JSON if input data contains quotes or special characters.

**Fix:** Use Jackson `ObjectMapper` (already on the classpath via Spring Boot) to build the explanation as a `Map<String, Object>` and serialize it:
```java
Map<String, Object> explanation = new LinkedHashMap<>();
explanation.put("topFactors", topFactors);
explanation.put("recommendations", recommendations);
String explanationJson = objectMapper.writeValueAsString(explanation);
```

---

### 3.3 Replace generic `Exception` catch with specific exception handling

**File:** `backend/data-service/src/main/java/com/water/data/service/RiskAssessmentService.java` (lines ~93–96)

**Problem:** `catch (Exception e)` swallows all exceptions including `OutOfMemoryError`, programming errors, etc., hiding bugs.

**Fix:** Catch only the specific checked exceptions that can reasonably be expected (e.g., `DataAccessException`, `IOException`), and let unexpected exceptions propagate:
```java
catch (DataAccessException e) {
    log.error("Database error during risk assessment for communityId={}", communityId, e);
    throw new RiskAssessmentException("Failed to retrieve data for assessment", e);
}
```

---

### 3.4 Fix deprecated JWT parsing method

**File:** `backend/api-gateway/src/main/java/com/water/gateway/filter/JwtValidationFilter.java`

**Problem:** Uses `.parseClaimsJws()` which is deprecated in JJWT 0.12.x in favor of `.parseSignedClaims()`.

**Fix:** Update the JWT parsing call:
```java
// Old (deprecated):
Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();

// New:
Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
```
Also ensure the key is typed as `SecretKey` (from `Keys.hmacShaKeyFor(...)`) rather than a plain `String`.

---

### 3.5 Fix N+1 / full-table scan in risk assessment

**File:** `backend/data-service/src/main/java/com/water/data/service/RiskAssessmentService.java`

**Problem:** `communityDataRepository.findAll()` loads all community records into memory. For large datasets this will cause out-of-memory errors and slow response times.

**Fix:**
- Add pagination: `communityDataRepository.findAll(PageRequest.of(0, 1000))` or, better, process by user's uploaded data only.
- Add a query method `findByUserId(UUID userId)` to `CommunityDataRepository` so the risk assessment only loads data belonging to the requesting user.
- Add a `@Query` with an index hint or ensure the `user_id` column is indexed in the database schema.

---

## Priority 4 — Configuration & Deployment

### 4.1 Disable Swagger UI in production profile

**File:** `backend/data-service/src/main/resources/application.yml`

**Problem:** `springdoc.swagger-ui.enabled: true` is set with no profile guard. The API documentation is visible to anyone in production.

**Fix:** Move Swagger enablement to a dev-only profile:
```yaml
# In application.yml (default - production):
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false

# In application-dev.yml (dev only):
springdoc:
  swagger-ui:
    enabled: true
  api-docs:
    enabled: true
```

---

### 4.2 Change log level to INFO in production

**Files:**
- `backend/auth-service/src/main/resources/application.yml`
- `backend/data-service/src/main/resources/application.yml`

**Problem:** Both files set `logging.level.com.water: DEBUG`. DEBUG logs can expose user data, query parameters, and internal state in production.

**Fix:** Change to INFO and move DEBUG to `application-dev.yml`:
```yaml
# application.yml:
logging:
  level:
    com.water: INFO

# application-dev.yml:
logging:
  level:
    com.water: DEBUG
```

---

### 4.3 Add OWASP Dependency-Check to CI/CD pipeline

**File:** `.github/workflows/ci-cd.yml`

**Problem:** The CI/CD pipeline builds and tests code but does not scan Java dependencies for known CVEs.

**Fix:** Add a step to the workflow after the build step:
```yaml
- name: OWASP Dependency Check
  run: |
    cd backend/auth-service
    mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7
    cd ../data-service
    mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7
```

---

## Priority 5 — Missing Tests

### 5.1 Add unit tests for RiskScoringService

**File to create:** `backend/data-service/src/test/java/com/water/data/service/RiskScoringServiceTest.java`

**Problem:** The risk scoring algorithm (`RiskScoringService.java`) has no unit tests. Any regression in the scoring logic is invisible.

**Tests to write:**
- `testLowRiskCommunityScoresBelow33()` — community with good water quality, nearby infrastructure, low population
- `testHighRiskCommunityScoresAbove67()` — community with poor water quality, no infrastructure, high population
- `testNullCoordinatesDoesNotThrowNPE()` — community with null lat/lng
- `testRecommendationsGeneratedForHighRisk()` — verify recommendations list is non-empty for HIGH risk score

---

### 5.2 Add unit tests for AuthService

**File to create:** `backend/auth-service/src/test/java/com/water/auth/service/AuthServiceTest.java`

**Tests to write:**
- `testRegisterNewUser_success()`
- `testRegisterDuplicateEmail_throwsDuplicateEmailException()`
- `testLoginWithWrongPassword_incrementsFailureCount()`
- `testLoginAfter5Failures_throwsAccountLockedException()`
- `testChangePassword_withCorrectCurrentPassword_succeeds()`

---

### 5.3 Add integration tests for key API endpoints

**File to create:** `backend/auth-service/src/test/java/com/water/auth/controller/AuthControllerIntegrationTest.java`

**Tests to write (using `@SpringBootTest` + `MockMvc` + `@Testcontainers` with PostgreSQL):**
- `POST /api/auth/register` — 201 on success, 409 on duplicate email
- `POST /api/auth/login` — 200 with JWT on success, 401 on bad credentials
- `GET /api/auth/me` — 200 with user info when authenticated, 401 when not
- `POST /api/auth/change-password` — 200 on success, 400 on wrong current password

---

## Priority 6 — README & Documentation Fixes

### 6.1 Fix README troubleshooting — wrong file upload size limit

**File:** `README.md` (line 858)

**Problem:** Troubleshooting section says "max 100MB" but the actual limit in the code and documentation elsewhere is 10MB per file.

**Fix:** Change line 858 from `Check file size (max 100MB)` to `Check file size (max 10MB per file)`.

---

### 6.2 Fix README CI/CD quality gate formatting

**File:** `README.md` (lines 823–831)

**Problem:** Quality gate checkboxes use `[X]` (no space) which does not render as a checked checkbox in GitHub Markdown. Should be `[x]` (lowercase) or add a space.

**Fix:** Change all `[X]` to `- [x]` so GitHub renders them as checked items.

---

### 6.3 Update README architecture diagram to include API Gateway

**File:** `README.md` (lines 167–189)

**Problem:** The architecture diagram shows requests going directly from the Frontend to Auth Service and Data Service. The `api-gateway` service is actually a Spring Boot service not a true gateway proxy, and the diagram doesn't show the real request flow accurately.

**Fix:** Update the diagram to reflect the actual deployment — the frontend calls the services directly (or through nginx proxy rules), and note clearly which service does what. Remove the implication of an API Gateway if one isn't used.

---

## Summary Table

| # | Area | Severity | File(s) |
|---|------|----------|---------|
| 1.1 | Remove hardcoded credential fallbacks | HIGH | `*/application.yml` (3 files) |
| 1.2 | Fix wildcard CORS | HIGH | `GatewayConfig.java`, `AuthController.java`, `application.yml`, `water_predictor.py` |
| 1.3 | Add rate limiting to auth endpoints | HIGH | `AuthController.java`, new filter |
| 1.4 | Fix password reset token expiry/single-use | HIGH | `AuthController.java`, schema |
| 2.1 | Rename `api-gateway` → `data-service` | MEDIUM | Directory rename + all references |
| 2.2 | Fix license inconsistency (MIT vs Apache 2.0) | MEDIUM | `README.md`, `LICENSE` |
| 2.3 | Add AI model service to Docker Compose | MEDIUM | `docker-compose.prod.yml` |
| 3.1 | Implement JWT refresh tokens | MEDIUM | `JwtService.java`, `AuthController.java` |
| 3.2 | Replace manual JSON string building | LOW | `RiskScoringService.java` |
| 3.3 | Replace generic Exception catch | LOW | `RiskAssessmentService.java` |
| 3.4 | Fix deprecated JWT parsing method | LOW | `JwtValidationFilter.java` |
| 3.5 | Fix full-table scan in risk assessment | MEDIUM | `RiskAssessmentService.java`, `CommunityDataRepository.java` |
| 4.1 | Disable Swagger in production | MEDIUM | `data-service/application.yml` |
| 4.2 | Change log level to INFO in production | MEDIUM | both `application.yml` files |
| 4.3 | Add OWASP dependency-check to CI | MEDIUM | `ci-cd.yml` |
| 5.1 | Add unit tests for RiskScoringService | HIGH | New test file |
| 5.2 | Add unit tests for AuthService | HIGH | New test file |
| 5.3 | Add integration tests for auth API | MEDIUM | New test file |
| 6.1 | Fix file size limit in README | LOW | `README.md` line 858 |
| 6.2 | Fix checkbox formatting in README | LOW | `README.md` lines 823–831 |
| 6.3 | Update architecture diagram | LOW | `README.md` lines 167–189 |
