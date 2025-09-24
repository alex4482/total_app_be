package com.work.total_app.utils;

import org.springframework.security.core.Authentication;

public interface TokenVerifier {
    Authentication verifyToken(String token);
}
