package com.work.total_app.services.authentication;

import com.work.total_app.models.authentication.*;
import com.work.total_app.models.user.User;
import com.work.total_app.repositories.TokenRepository;
import com.work.total_app.repositories.UserRepository;
import com.work.total_app.services.security.EmailVerificationService;
import com.work.total_app.services.security.LoginRateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalAuthServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenService tokenService;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginRateLimitService rateLimitService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private LocalAuthService authService;

    private BCryptPasswordEncoder bCrypt = new BCryptPasswordEncoder();
    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshDays", 7L);
        
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPasswordHash(bCrypt.encode("password123"));
        testUser.setEmail("test@example.com");
        testUser.setEnabled(true);
        testUser.setAccountLocked(false);
        testUser.setFailedLoginAttempts(0);
        testUser.setRequiresEmailVerification(false);
    }

    @Test
    void login_withValidCredentials_shouldReturnTokens() {
        // Given
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(rateLimitService.isIpRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(rateLimitService.isAccountLocked(testUser)).thenReturn(false);
        when(rateLimitService.requiresEmailVerification(testUser)).thenReturn(false);
        when(jwtService.issueAccess(anyString())).thenReturn("access_token");
        when(tokenService.randomToken(32)).thenReturn("refresh_token");
        when(tokenService.sha256Hex(anyString())).thenReturn("token_hash");

        // When
        AuthTokens tokens = authService.login(request, "127.0.0.1", "test-agent");

        // Then
        assertNotNull(tokens);
        assertNotNull(tokens.getIdToken());
        assertNotNull(tokens.getRefreshToken());
        assertNotNull(tokens.getSessionId());
        
        verify(rateLimitService).recordLoginAttempt(eq("testuser"), eq("127.0.0.1"), 
            eq("test-agent"), eq(true), isNull());
        verify(tokenRepository).save(any());
    }

    @Test
    void login_withInvalidPassword_shouldThrowUnauthorized() {
        // Given
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");
        when(rateLimitService.isIpRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(rateLimitService.isAccountLocked(testUser)).thenReturn(false);
        when(rateLimitService.requiresEmailVerification(testUser)).thenReturn(false);

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> authService.login(request, "127.0.0.1", "test-agent"));
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Credențiale invalide", exception.getReason());
        
        verify(rateLimitService).recordLoginAttempt(eq("testuser"), eq("127.0.0.1"), 
            eq("test-agent"), eq(false), eq("WRONG_PASSWORD"));
    }

    @Test
    void login_withNonExistentUser_shouldThrowUnauthorized() {
        // Given
        LoginRequest request = new LoginRequest("nonexistent", "password123");
        when(rateLimitService.isIpRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> authService.login(request, "127.0.0.1", "test-agent"));
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        
        verify(rateLimitService).recordLoginAttempt(eq("nonexistent"), eq("127.0.0.1"), 
            eq("test-agent"), eq(false), eq("USER_NOT_FOUND"));
    }

    @Test
    void login_withDisabledAccount_shouldThrowForbidden() {
        // Given
        LoginRequest request = new LoginRequest("testuser", "password123");
        testUser.setEnabled(false);
        
        when(rateLimitService.isIpRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> authService.login(request, "127.0.0.1", "test-agent"));
        
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Contul este dezactivat", exception.getReason());
        
        verify(rateLimitService).recordLoginAttempt(eq("testuser"), eq("127.0.0.1"), 
            eq("test-agent"), eq(false), eq("ACCOUNT_DISABLED"));
    }

    @Test
    void login_withRateLimitedIp_shouldThrowTooManyRequests() {
        // Given
        LoginRequest request = new LoginRequest("testuser", "password123");
        when(rateLimitService.isIpRateLimited("127.0.0.1")).thenReturn(true);

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> authService.login(request, "127.0.0.1", "test-agent"));
        
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        
        verify(rateLimitService).recordLoginAttempt(eq("testuser"), eq("127.0.0.1"), 
            eq("test-agent"), eq(false), eq("IP_RATE_LIMITED"));
    }

    @Test
    void login_whenEmailVerificationRequired_shouldThrowForbidden() {
        // Given
        LoginRequest request = new LoginRequest("testuser", "password123");
        testUser.setRequiresEmailVerification(true);
        
        when(rateLimitService.isIpRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(rateLimitService.isAccountLocked(testUser)).thenReturn(false);
        when(rateLimitService.requiresEmailVerification(testUser)).thenReturn(true);

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> authService.login(request, "127.0.0.1", "test-agent"));
        
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("email"));
        
        verify(rateLimitService).recordLoginAttempt(eq("testuser"), eq("127.0.0.1"), 
            eq("test-agent"), eq(false), eq("REQUIRES_EMAIL_VERIFICATION"));
    }

    @Test
    void registerUser_withValidData_shouldCreateUser() {
        // Given
        RegisterUserRequest request = new RegisterUserRequest("newuser", "password123", "new@example.com");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User createdUser = authService.registerUser(request);

        // Then
        assertNotNull(createdUser);
        assertEquals("newuser", createdUser.getUsername());
        assertEquals("new@example.com", createdUser.getEmail());
        assertTrue(createdUser.isEnabled());
        
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_withExistingUsername_shouldThrowConflict() {
        // Given
        RegisterUserRequest request = new RegisterUserRequest("existinguser", "password123", "new@example.com");
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> authService.registerUser(request));
        
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Username-ul este deja folosit", exception.getReason());
    }

    @Test
    void registerUser_withShortPassword_shouldThrowBadRequest() {
        // Given
        RegisterUserRequest request = new RegisterUserRequest("newuser", "short", "new@example.com");

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> authService.registerUser(request));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("8 caractere"));
    }

    @Test
    void changePassword_withCorrectOldPassword_shouldUpdatePassword() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest("password123", "newpassword123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        authService.changePassword(testUser, request);

        // Then
        verify(userRepository).save(testUser);
        assertTrue(new BCryptPasswordEncoder().matches("newpassword123", testUser.getPasswordHash()));
    }

    @Test
    void changePassword_withIncorrectOldPassword_shouldThrowUnauthorized() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest("wrongpassword", "newpassword123");

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> authService.changePassword(testUser, request));
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Parola veche este incorectă", exception.getReason());
    }

    @Test
    void loginWithEmail_withValidCode_shouldReturnTokens() {
        // Given
        LoginWithEmailRequest request = new LoginWithEmailRequest(
            "testuser", "password123", "test@example.com", "123456");
        
        when(rateLimitService.isIpRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(emailVerificationService.verifyCode(testUser, "123456")).thenReturn(true);
        when(jwtService.issueAccess(anyString())).thenReturn("access_token");
        when(tokenService.randomToken(32)).thenReturn("refresh_token");
        when(tokenService.sha256Hex(anyString())).thenReturn("token_hash");

        // When
        AuthTokens tokens = authService.loginWithEmail(request, "127.0.0.1", "test-agent");

        // Then
        assertNotNull(tokens);
        assertNotNull(tokens.getIdToken());
        
        verify(rateLimitService).recordLoginAttempt(eq("testuser"), eq("127.0.0.1"), 
            eq("test-agent"), eq(true), isNull());
        verify(rateLimitService).resetEmailVerificationRequirement(testUser);
    }

    @Test
    void loginWithEmail_withInvalidCode_shouldThrowUnauthorized() {
        // Given
        LoginWithEmailRequest request = new LoginWithEmailRequest(
            "testuser", "password123", "test@example.com", "000000");
        
        when(rateLimitService.isIpRateLimited(anyString())).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(emailVerificationService.verifyCode(testUser, "000000")).thenReturn(false);

        // When & Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, 
            () -> authService.loginWithEmail(request, "127.0.0.1", "test-agent"));
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Cod de verificare invalid"));
        
        verify(rateLimitService).recordLoginAttempt(eq("testuser"), eq("127.0.0.1"), 
            eq("test-agent"), eq(false), eq("INVALID_VERIFICATION_CODE"));
    }
}

