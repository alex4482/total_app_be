package com.work.total_app.services.authentication;

import com.work.total_app.models.authentication.*;
import com.work.total_app.models.user.User;
import com.work.total_app.repositories.TokenRepository;
import com.work.total_app.repositories.UserRepository;
import com.work.total_app.services.security.EmailVerificationService;
import com.work.total_app.services.security.LoginRateLimitService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@Log4j2
public class LocalAuthService implements AuthenticationService {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoginRateLimitService rateLimitService;

    @Autowired
    private EmailVerificationService emailVerificationService;
    
    @Autowired
    private com.work.total_app.services.security.LoginDelayService loginDelayService;
    
    @Autowired
    private com.work.total_app.services.security.IpBlacklistService ipBlacklistService;

    private final BCryptPasswordEncoder bCrypt = new BCryptPasswordEncoder();

    @Value("${app.jwt.refresh-ttl-days}")
    private long refreshDays;

    /**
     * Login standard cu username și parolă
     */
    @Override
    @Transactional
    public AuthTokens login(LoginRequest req, String ip, String userAgent) {
        String username = req.username();
        String password = req.password();
        
        // Verifică IP blacklist (atacuri persistente)
        if (ipBlacklistService.isBlacklisted(ip)) {
            long minutesLeft = ipBlacklistService.getMinutesUntilUnblock(ip);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "IP-ul tău a fost blocat temporar din cauza activității suspicioase. " +
                "Vei putea încerca din nou în " + minutesLeft + " minute.");
        }
        
        // Aplică delay progresiv pentru a preveni brute-force
        loginDelayService.applyDelay(ip);

        // Verifică rate limiting pe IP
        if (rateLimitService.isIpRateLimited(ip)) {
            // Check if this IP should be blacklisted
            ipBlacklistService.checkAndBlacklistIfNeeded(ip);
            loginDelayService.recordFailedAttempt(ip);
            rateLimitService.recordLoginAttempt(username, ip, userAgent, false, "IP_RATE_LIMITED");
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, 
                "Prea multe încercări eșuate. Te rugăm să aștepți 15 minute.");
        }

        // Caută userul
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                rateLimitService.recordLoginAttempt(username, ip, userAgent, false, "USER_NOT_FOUND");
                return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credențiale invalide");
            });

        // Verifică dacă contul este activ
        if (!user.isEnabled()) {
            rateLimitService.recordLoginAttempt(username, ip, userAgent, false, "ACCOUNT_DISABLED");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contul este dezactivat");
        }

        // Verifică dacă contul este blocat temporar
        if (rateLimitService.isAccountLocked(user)) {
            rateLimitService.recordLoginAttempt(username, ip, userAgent, false, "ACCOUNT_LOCKED");
            long minutesLeft = Duration.between(Instant.now(), user.getAccountLockedUntil()).toMinutes();
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "Contul este blocat temporar. Încearcă din nou în " + minutesLeft + " minute.");
        }

        // Verifică parola
        if (!bCrypt.matches(password, user.getPasswordHash())) {
            loginDelayService.recordFailedAttempt(ip);
            rateLimitService.recordLoginAttempt(username, ip, userAgent, false, "WRONG_PASSWORD");
            
            // Verifică dacă userul trebuie să folosească verificare prin email
            if (rateLimitService.requiresEmailVerification(user)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Prea multe încercări eșuate. Te rugăm să te autentifici cu cod trimis pe email.");
            }
            
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credențiale invalide");
        }

        // Verifică dacă userul trebuie să folosească verificare prin email
        if (rateLimitService.requiresEmailVerification(user)) {
            rateLimitService.recordLoginAttempt(username, ip, userAgent, false, "REQUIRES_EMAIL_VERIFICATION");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "Prea multe încercări eșuate. Te rugăm să te autentifici cu cod trimis pe email.");
        }

        // Login reușit - resetează delay counters
        loginDelayService.resetAttempts(ip);
        rateLimitService.recordLoginAttempt(username, ip, userAgent, true, null);
        return generateTokens(user.getId().toString(), ip, userAgent);
    }

    /**
     * Login cu verificare prin email (după 6 încercări eșuate)
     */
    @Override
    @Transactional
    public AuthTokens loginWithEmail(LoginWithEmailRequest req, String ip, String userAgent) {
        String username = req.username();
        String password = req.password();
        String email = req.email();
        String verificationCode = req.verificationCode();

        // Verifică rate limiting pe IP
        if (rateLimitService.isIpRateLimited(ip)) {
            rateLimitService.recordLoginAttempt(username, ip, userAgent, false, "IP_RATE_LIMITED");
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, 
                "Prea multe încercări eșuate. Te rugăm să aștepți 15 minute.");
        }

        // Caută userul
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                rateLimitService.recordLoginAttempt(username, ip, userAgent, false, "USER_NOT_FOUND");
                return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credențiale invalide");
            });

        // Verifică dacă contul este activ
        if (!user.isEnabled()) {
            rateLimitService.recordLoginAttempt(username, ip, userAgent, false, "ACCOUNT_DISABLED");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contul este dezactivat");
        }

        // Verifică parola
        if (!bCrypt.matches(password, user.getPasswordHash())) {
            rateLimitService.recordLoginAttempt(username, ip, userAgent, false, "WRONG_PASSWORD");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credențiale invalide");
        }

        // Verifică codul de verificare
        if (verificationCode == null || !emailVerificationService.verifyCode(user, verificationCode)) {
            rateLimitService.recordLoginAttempt(username, ip, userAgent, false, "INVALID_VERIFICATION_CODE");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cod de verificare invalid sau expirat");
        }

        // Login reușit - resetează flag-ul de verificare email
        rateLimitService.recordLoginAttempt(username, ip, userAgent, true, null);
        rateLimitService.resetEmailVerificationRequirement(user);
        
        return generateTokens(user.getId().toString(), ip, userAgent);
    }

    /**
     * Solicită un cod de verificare prin email
     */
    @Override
    @Transactional
    public boolean requestEmailVerificationCode(RequestEmailCodeRequest req, String ip) {
        String username = req.username();
        String password = req.password();
        String email = req.email();

        // Caută userul
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credențiale invalide"));

        // Verifică parola
        if (!bCrypt.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credențiale invalide");
        }

        // Verifică dacă userul chiar trebuie să folosească verificare prin email
        if (!rateLimitService.requiresEmailVerification(user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Verificarea prin email nu este necesară pentru acest cont");
        }

        // Generează și trimite codul
        boolean sent = emailVerificationService.generateAndSendCode(user, email, ip);
        
        if (!sent) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Nu s-a putut trimite codul de verificare. Verifică dacă emailul este în whitelist.");
        }

        return true;
    }

    /**
     * Refresh token
     */
    @Override
    @Transactional
    public AuthTokens refreshToken(RefreshTokenRequest req) {
        String rawRefresh = req.refreshToken();
        String hash = tokenService.sha256Hex(rawRefresh);
        
        RefreshTokenState refreshTokenData = tokenRepository.findByAnyHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh"));

        Instant now = Instant.now();
        if (now.isAfter(refreshTokenData.getExpiresAt()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh expired");

        // Get user to determine role for JWT
        User user = userRepository.findById(UUID.fromString(refreshTokenData.getUserId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
        
        // Check if user is deleted or disabled
        if (user.isDeleted() || !user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user account is disabled or deleted");
        }
        
        // If client resends the immediately-previous token (retry), allow once without rotating again
        if (hash.equals(refreshTokenData.getPreviousTokenHash())) {
            refreshTokenData.setPreviousTokenHash(null);
            tokenRepository.saveAndFlush(refreshTokenData);

            return new AuthTokens(
                jwtService.issueAccessWithRole(refreshTokenData.getSessionId(), user.getRole()),
                null,
                refreshTokenData.getSessionId()
            );
        }

        // Normal rotation: current token must match
        if (!hash.equals(refreshTokenData.getTokenHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh reuse/compromise");

        // Rotate in place
        String newRaw  = tokenService.randomToken(32);
        String newHash = tokenService.sha256Hex(newRaw);
        refreshTokenData.setPreviousTokenHash(refreshTokenData.getTokenHash());
        refreshTokenData.setTokenHash(newHash);
        refreshTokenData.setExpiresAt(now.plus(Duration.ofDays(refreshDays)));
        tokenRepository.saveAndFlush(refreshTokenData);

        return new AuthTokens(
            jwtService.issueAccessWithRole(refreshTokenData.getSessionId(), user.getRole()), 
            newRaw,
            refreshTokenData.getSessionId()
        );
    }

    /**
     * Logout
     */
    @Override
    @Transactional
    public void logout(String sessionId) {
        tokenRepository.findById(sessionId).ifPresent(s -> {
            s.setRevokedAfter(Instant.now().minusSeconds(30));
            s.setExpiresAt(Instant.now().minusSeconds(1));
            s.setTokenHash("x");
            s.setPreviousTokenHash(null);
            tokenRepository.save(s);
        });
    }

    /**
     * Înregistrează un user nou
     */
    @Override
    @Transactional
    public User registerUser(RegisterUserRequest req) {
        // Validări
        if (req.username() == null || req.username().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username-ul este obligatoriu");
        }
        
        if (req.password() == null || req.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parola trebuie să aibă cel puțin 8 caractere");
        }
        
        // Verifică dacă username-ul există deja
        if (userRepository.existsByUsername(req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username-ul este deja folosit");
        }
        
        // Verifică dacă emailul există deja (dacă e furnizat)
        if (req.email() != null && !req.email().trim().isEmpty()) {
            if (userRepository.existsByEmail(req.email())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Emailul este deja folosit");
            }
        }
        
        // Creează userul
        User user = new User();
        user.setUsername(req.username());
        user.setPasswordHash(bCrypt.encode(req.password()));
        user.setEmail(req.email());
        user.setEnabled(true);
        
        user = userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());
        
        return user;
    }

    /**
     * Schimbă parola
     */
    @Override
    @Transactional
    public void changePassword(User user, ChangePasswordRequest req) {
        // Verifică parola veche
        if (!bCrypt.matches(req.oldPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Parola veche este incorectă");
        }
        
        // Validează parola nouă
        if (req.newPassword() == null || req.newPassword().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parola nouă trebuie să aibă cel puțin 8 caractere");
        }
        
        // Actualizează parola
        user.setPasswordHash(bCrypt.encode(req.newPassword()));
        userRepository.save(user);
        
        log.info("Password changed for user: {}", user.getUsername());
    }

    /**
     * Generează token-uri de acces și refresh
     */
    private AuthTokens generateTokens(String userId, String ip, String userAgent) {
        String sessionId = UUID.randomUUID().toString();
        String raw = tokenService.randomToken(32);
        String hash = tokenService.sha256Hex(raw);
        Instant now = Instant.now();
        
        // Get user to determine role-based expiration
        User user = userRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        RefreshTokenState rt = new RefreshTokenState();
        rt.setSessionId(sessionId);
        rt.setUserId(userId);
        rt.setTokenHash(hash);
        rt.setCreatedAt(now);
        rt.setRevokedAfter(Instant.EPOCH);
        rt.setPreviousTokenHash(null);
        rt.setExpiresAt(now.plus(Duration.ofDays(refreshDays)));
        rt.setIpAddress(ip);
        rt.setUserAgent(userAgent);
        tokenRepository.save(rt);

        String access = jwtService.issueAccessWithRole(sessionId, user.getRole());
        return new AuthTokens(access, raw, sessionId);
    }
}
