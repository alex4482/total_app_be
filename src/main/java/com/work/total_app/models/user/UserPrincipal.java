package com.work.total_app.models.user;

import java.util.UUID;

public record UserPrincipal(String provider,
        String subject,
        String sessionId,
        String email,
        UUID userId,
        UserRole role) {
    public UserPrincipal {
        if (provider == null || subject == null) throw new IllegalArgumentException("null principal fields");
        if (userId == null) throw new IllegalArgumentException("userId cannot be null");
        if (role == null) throw new IllegalArgumentException("role cannot be null");
    }
    
    /**
     * Checks if this principal has ADMIN role.
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
    
    /**
     * Checks if this principal has SUPERUSER role.
     */
    public boolean isSuperUser() {
        return role == UserRole.SUPERUSER;
    }
    
    /**
     * Checks if this principal has MINIUSER role.
     */
    public boolean isMiniUser() {
        return role == UserRole.MINIUSER;
    }
}
