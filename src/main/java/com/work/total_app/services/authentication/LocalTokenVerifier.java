package com.work.total_app.services.authentication;

import com.work.total_app.models.user.UserPrincipal;
import com.work.total_app.repositories.TokenRepository;
import com.work.total_app.utils.TokenVerifier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class LocalTokenVerifier implements TokenVerifier {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenRepository tokenRepository;

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

            UserPrincipal principal = new UserPrincipal("local", c.getSubject(), sid, null);
            return new UsernamePasswordAuthenticationToken(principal, token, List.of());
        }
        catch (Exception e) {
            // This provider definitely tried to parse it; signal “invalid for me”.
            throw (e instanceof BadCredentialsException) ? (BadCredentialsException)e
                    : new BadCredentialsException("invalid local jwt", e);
        }
    }
}
