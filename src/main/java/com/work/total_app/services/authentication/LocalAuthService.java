package com.work.total_app.services.authentication;

import com.work.total_app.models.authentication.AuthTokens;
import com.work.total_app.models.authentication.LoginRequest;
import com.work.total_app.models.authentication.RefreshTokenState;
import com.work.total_app.models.authentication.RefreshTokenRequest;
import com.work.total_app.repositories.TokenRepository;
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
public class LocalAuthService implements AuthenticationService {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TokenRepository tokenRepository;

    private final BCryptPasswordEncoder bCrypt = new BCryptPasswordEncoder();

    @Value("${app.auth.universal-password-hash}")
    private String universalHash;
    @Value("${app.jwt.refresh-ttl-days}")
    private long refreshDays;

    @Override
    @Transactional
    public AuthTokens login(LoginRequest req) {
        String password = req.password();
        if(!bCrypt.matches(password, universalHash))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        String sessionId = UUID.randomUUID().toString();
        String raw = tokenService.randomToken(32);
        String hash = tokenService.sha256Hex(raw);
        Instant now = Instant.now();

        RefreshTokenState rt = new RefreshTokenState();
        rt.setSessionId(sessionId);
        rt.setTokenHash(hash);
        rt.setCreatedAt(now);
        rt.setRevokedAfter(Instant.EPOCH);
        rt.setPreviousTokenHash(null);
        rt.setExpiresAt(now.plus(Duration.ofDays(refreshDays)));
        // rt.setIp(ip);
        // rt.setUserAgent(ua);
        tokenRepository.save(rt);

        String access = jwtService.issueAccess(sessionId);
        return new AuthTokens(access, raw, sessionId);
    }

    @Override
    @Transactional
    public AuthTokens refreshToken(RefreshTokenRequest req) {
        String rawRefresh = req.refreshToken();
        String hash;
        hash = tokenService.sha256Hex(rawRefresh);
        RefreshTokenState refreshTokenData = tokenRepository.findByAnyHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh"));

        Instant now = Instant.now();
        if (now.isAfter(refreshTokenData.getExpiresAt()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "refresh expired");

        // If client resends the immediately-previous token (retry), allow once without rotating again
        if (hash.equals(refreshTokenData.getPreviousTokenHash())) {
            // Idempotent retry: allow exactly once
            refreshTokenData.setPreviousTokenHash(null);       // <-- invalidate previous token now
            tokenRepository.saveAndFlush(refreshTokenData);

            return new AuthTokens(jwtService.issueAccess(refreshTokenData.getSessionId()),
                    null,             // if using HttpOnly cookie for refresh
                    refreshTokenData.getSessionId());
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

        return new AuthTokens(jwtService.issueAccess(refreshTokenData.getSessionId()), newRaw,
                refreshTokenData.getSessionId());
    }

    @Transactional
    public void logout(String sessionId) {
        // Kill future refreshes and cut off access tokens immediately
        tokenRepository.findById(sessionId).ifPresent(s -> {
            s.setRevokedAfter(Instant.now().minusSeconds(30)); // tiny skew cushion
            s.setExpiresAt(Instant.now().minusSeconds(1));     // stop refresh path
            s.setTokenHash("x"); s.setPreviousTokenHash(null); // optional: make unusable
            tokenRepository.save(s);
        });
    }
}
