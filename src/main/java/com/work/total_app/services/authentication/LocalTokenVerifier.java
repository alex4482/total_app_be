package com.work.total_app.services.authentication;

import com.work.total_app.models.authentication.RefreshTokenState;
import com.work.total_app.models.user.User;
import com.work.total_app.models.user.UserPrincipal;
import com.work.total_app.models.user.UserRole;
import com.work.total_app.repositories.TokenRepository;
import com.work.total_app.repositories.UserRepository;
import com.work.total_app.utils.TokenVerifier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class LocalTokenVerifier implements TokenVerifier {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenRepository tokenRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Override
    public Authentication verifyToken(String token) {
        try {
            Jws<Claims> jws = jwtService.parse(token);         // verifies signature + exp
            Claims c = jws.getPayload();
            String sid = c.get("sid", String.class);
            Instant iat = c.getIssuedAt() == null ? null : c.getIssuedAt().toInstant();
            if (sid == null || iat == null) throw new BadCredentialsException("missing sid/iat");

            // check db for manual revocations
            Instant cut = tokenRepository.findRevokedAfter(sid).orElse(Instant.EPOCH);
            if (!iat.isAfter(cut)) throw new BadCredentialsException("session revoked");
            
            // Get user from session
            RefreshTokenState session = tokenRepository.findById(sid)
                .orElseThrow(() -> new BadCredentialsException("session not found"));
            
            UUID userId = UUID.fromString(session.getUserId());
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("user not found"));
            
            // Check if user is deleted or disabled
            if (user.isDeleted() || !user.isEnabled()) {
                throw new BadCredentialsException("user account is disabled or deleted");
            }
            
            // Extract role from JWT (or use user's current role from DB)
            String roleStr = c.get("role", String.class);
            UserRole role = roleStr != null ? UserRole.valueOf(roleStr) : user.getRole();
            
            UserPrincipal principal = new UserPrincipal(
                "local", 
                c.getSubject(), 
                sid, 
                user.getEmail(),
                userId,
                role
            );
            
            // Create authorities from role
            List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
            );
            
            return new UsernamePasswordAuthenticationToken(principal, token, authorities);
        }
        catch (Exception e) {
            // This provider definitely tried to parse it; signal "invalid for me".
            throw (e instanceof BadCredentialsException) ? (BadCredentialsException)e
                    : new BadCredentialsException("invalid local jwt", e);
        }
    }
}
