package com.work.total_app.controllers;

import com.work.total_app.config.SecurityProperties;
import com.work.total_app.models.authentication.*;
import com.work.total_app.models.user.User;
import com.work.total_app.models.user.UserPrincipal;
import com.work.total_app.repositories.UserRepository;
import com.work.total_app.services.authentication.AuthenticationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Controller
@RequestMapping("/auth")
@Log4j2
public class AuthenticationController {

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SecurityProperties securityProperties;

    /**
     * Login standard cu username și parolă
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest authRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        if (authRequest == null || authRequest.password() == null) {
            log.info("Returning forbidden for request <{}>", authRequest);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        String ip = getClientIP(request);
        String userAgent = request.getHeader("User-Agent");
        
        AuthTokens tokens;
        try {
            tokens = authService.login(authRequest, ip, userAgent);
        } catch (ResponseStatusException e) {
            log.warn("Login failed for user '{}': {}", authRequest.username(), e.getReason());
            return ResponseEntity.status(e.getStatusCode()).build();
        }
        
        log.info("Login successful for user '{}'", authRequest.username());
        
        // Set refresh token as HttpOnly cookie
        setRefreshTokenCookie(response, tokens.getRefreshToken());
        
        AuthenticationResponse authResponse = AuthenticationResponse.builder()
                .tokens(new AuthTokens(tokens.getIdToken(), null, tokens.getSessionId()))
                .build();
        
        return new ResponseEntity<>(authResponse, HttpStatus.ACCEPTED);
    }

    /**
     * Login cu verificare prin email (după 6 încercări eșuate)
     */
    @PostMapping("/login-with-email")
    public ResponseEntity<AuthenticationResponse> loginWithEmail(
            @RequestBody LoginWithEmailRequest authRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        if (authRequest == null || authRequest.password() == null || authRequest.verificationCode() == null) {
            log.info("Returning forbidden for login-with-email request");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        String ip = getClientIP(request);
        String userAgent = request.getHeader("User-Agent");
        
        AuthTokens tokens;
        try {
            tokens = authService.loginWithEmail(authRequest, ip, userAgent);
        } catch (ResponseStatusException e) {
            log.warn("Login with email failed for user '{}': {}", authRequest.username(), e.getReason());
            return ResponseEntity.status(e.getStatusCode()).build();
        }
        
        log.info("Login with email successful for user '{}'", authRequest.username());
        
        // Set refresh token as HttpOnly cookie
        setRefreshTokenCookie(response, tokens.getRefreshToken());
        
        AuthenticationResponse authResponse = AuthenticationResponse.builder()
                .tokens(new AuthTokens(tokens.getIdToken(), null, tokens.getSessionId()))
                .build();
        
        return new ResponseEntity<>(authResponse, HttpStatus.ACCEPTED);
    }

    /**
     * Solicită un cod de verificare prin email
     */
    @PostMapping("/request-email-code")
    public ResponseEntity<Map<String, String>> requestEmailCode(
            @Valid @RequestBody RequestEmailCodeRequest req,
            HttpServletRequest request) {
        
        if (req == null || req.username() == null || req.password() == null || req.email() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        
        String ip = getClientIP(request);
        
        try {
            boolean sent = authService.requestEmailVerificationCode(req, ip);
            if (sent) {
                return ResponseEntity.ok(Map.of("message", "Cod de verificare trimis pe email"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Nu s-a putut trimite codul de verificare"));
            }
        } catch (ResponseStatusException e) {
            log.warn("Request email code failed: {}", e.getReason());
            return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("error", e.getReason()));
        }
    }

    /**
     * Refresh token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest req,
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken,
            HttpServletResponse response) {
        
        // Încearcă să obții refresh token-ul din cookie sau din body
        String refreshToken = cookieRefreshToken;
        if (refreshToken == null && req != null) {
            refreshToken = req.refreshToken();
        }
        
        if (refreshToken == null) {
            log.info("No refresh token provided");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        AuthTokens tokens;
        try {
            tokens = authService.refreshToken(new RefreshTokenRequest(refreshToken));
        } catch (ResponseStatusException e) {
            log.warn("Refresh token failed: {}", e.getReason());
            clearRefreshTokenCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Setează noul refresh token ca HttpOnly cookie (dacă a fost rotated)
        if (tokens.getRefreshToken() != null) {
            setRefreshTokenCookie(response, tokens.getRefreshToken());
        }
        
        AuthenticationResponse authResponse = AuthenticationResponse.builder()
                .tokens(new AuthTokens(tokens.getIdToken(), null, tokens.getSessionId()))
                .build();
        
        return new ResponseEntity<>(authResponse, HttpStatus.ACCEPTED);
    }

    /**
     * Logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            authService.logout(principal.sessionId());
            log.info("User '{}' logged out", principal.subject());
        }
        
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    /**
     * Register user
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterUserRequest req) {
        try {
            User user = authService.registerUser(req);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "User created successfully", "username", user.getUsername()));
        } catch (ResponseStatusException e) {
            log.warn("Registration failed: {}", e.getReason());
            return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("error", e.getReason()));
        }
    }

    /**
     * Change password
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Get user from repository using username from principal
            User user = userRepository.findByUsername(principal.subject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            
            // Change password
            authService.changePassword(user, req);
            
            log.info("Password changed successfully for user: {}", principal.subject());
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (ResponseStatusException e) {
            log.warn("Change password failed: {}", e.getReason());
            return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("error", e.getReason()));
        }
    }

    /**
     * Helper: Set refresh token as HttpOnly cookie
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        if (refreshToken == null) return;
        
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(securityProperties.getCookies().isSecure()); // Configurable per environment
        cookie.setPath("/auth");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        cookie.setAttribute("SameSite", securityProperties.getCookies().getSameSite());
        
        response.addCookie(cookie);
    }

    /**
     * Helper: Clear refresh token cookie
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(securityProperties.getCookies().isSecure());
        cookie.setPath("/auth");
        cookie.setMaxAge(0);
        
        response.addCookie(cookie);
    }

    /**
     * Helper: Get client IP address
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
