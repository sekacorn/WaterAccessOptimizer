package com.water.auth.service;

import com.water.auth.dto.LoginRequest;
import com.water.auth.dto.RegisterRequest;
import com.water.auth.exception.AccountLockedException;
import com.water.auth.exception.DuplicateEmailException;
import com.water.auth.exception.InvalidCredentialsException;
import com.water.auth.repository.PasswordResetTokenRepository;
import com.water.auth.repository.RefreshTokenRepository;
import com.water.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService authService;
    private UserRepository userRepository;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepository();
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);

        authService = new AuthService(
            userRepository,
            jwtService,
            new AuditLogService(),
            new RefreshTokenRepository(),
            new PasswordResetTokenRepository()
        );

        request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("User-Agent")).thenReturn("JUnit");
        Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void testRegisterNewUser_success() {
        RegisterRequest request = registerRequest("alice@example.com");

        var response = authService.register(request, this.request);

        assertNotNull(response.getToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("alice@example.com", response.getUser().getEmail());
    }

    @Test
    void testRegisterDuplicateEmail_throwsDuplicateEmailException() {
        RegisterRequest request = registerRequest("dup@example.com");
        authService.register(request, this.request);

        assertThrows(DuplicateEmailException.class, () -> authService.register(request, this.request));
    }

    @Test
    void testLoginWithWrongPassword_incrementsFailureCount() {
        RegisterRequest request = registerRequest("wrongpass@example.com");
        authService.register(request, this.request);

        LoginRequest login = new LoginRequest();
        login.setEmail("wrongpass@example.com");
        login.setPassword("Wrong123");

        assertThrows(InvalidCredentialsException.class, () -> authService.login(login, this.request));
        assertEquals(1, userRepository.findByEmail("wrongpass@example.com").orElseThrow().getFailedLoginAttempts());
    }

    @Test
    void testLoginAfter5Failures_throwsAccountLockedException() {
        RegisterRequest request = registerRequest("locked@example.com");
        authService.register(request, this.request);

        LoginRequest login = new LoginRequest();
        login.setEmail("locked@example.com");
        login.setPassword("Wrong123");

        for (int i = 0; i < 4; i++) {
            assertThrows(InvalidCredentialsException.class, () -> authService.login(login, this.request));
        }

        assertThrows(AccountLockedException.class, () -> authService.login(login, this.request));
    }

    @Test
    void testChangePassword_withCorrectCurrentPassword_succeeds() {
        RegisterRequest request = registerRequest("changepw@example.com");
        var authResponse = authService.register(request, this.request);

        assertDoesNotThrow(() -> authService.changePassword(authResponse.getToken(), "Valid123", "NewValid123"));
    }

    private RegisterRequest registerRequest(String email) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword("Valid123");
        request.setFirstName("Alice");
        request.setLastName("Tester");
        request.setOrganization("Org");
        return request;
    }
}
