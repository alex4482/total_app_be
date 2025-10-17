package com.work.total_app.services.authentication;

import com.work.total_app.models.runtime_errors.ValidationException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class TokenService {
    private final SecureRandom rnd = new SecureRandom();
    public String randomToken(int bytes) {
        byte[] b=new byte[bytes]; rnd.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
    public String sha256Hex(String raw) {
        if (raw == null)
        {
            throw new ValidationException("No text given.");
        }
        try {
            var md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new RuntimeException(e); }
    }
}
