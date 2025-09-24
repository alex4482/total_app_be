package com.work.total_app.models.user;

public record UserPrincipal(String provider,
        String subject,
        String sessionId,
        String email) {
    public UserPrincipal {
        if (provider == null || subject == null) throw new IllegalArgumentException("null principal fields");
    }
}
