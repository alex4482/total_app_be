package com.work.total_app.models.user;

/**
 * User roles for authorization.
 * 
 * <p><strong>ADMIN:</strong>
 * <ul>
 *   <li>Full system access</li>
 *   <li>Hard delete capability</li>
 *   <li>2FA required (email verification)</li>
 *   <li>Exactly 1 admin user in system</li>
 *   <li>Excluded from all backups (security)</li>
 *   <li>Only role that can restore backups</li>
 *   <li>Session timeout: 15 minutes</li>
 *   <li>Password: 12+ chars, complex (uppercase, lowercase, number, special)</li>
 * </ul>
 * 
 * <p><strong>SUPERUSER:</strong>
 * <ul>
 *   <li>Access to all endpoints (except admin-only)</li>
 *   <li>Soft delete only (recoverable)</li>
 *   <li>No 2FA required</li>
 *   <li>Unlimited quantity (typically ~2)</li>
 *   <li>Can create backups (included in backups)</li>
 *   <li>Session timeout: 60 minutes</li>
 *   <li>Password: 6+ chars</li>
 * </ul>
 * 
 * <p><strong>MINIUSER:</strong>
 * <ul>
 *   <li>Limited access to specific endpoints:</li>
 *   <li>  - File operations (/files/**)</li>
 *   <li>  - Reminders (/reminders/**)</li>
 *   <li>  - Own backups only (/backups - mini DB)</li>
 *   <li>Soft delete only</li>
 *   <li>No 2FA required</li>
 *   <li>Unlimited quantity (typically ~2)</li>
 *   <li>Uses separate mini database (optional)</li>
 *   <li>Session timeout: 120 minutes</li>
 *   <li>Password: 6+ chars</li>
 * </ul>
 */
public enum UserRole {
    /**
     * Administrator - Full access, hard delete, 2FA required.
     * Only 1 admin allowed in system.
     */
    ADMIN,
    
    /**
     * Super user - Full access (except admin operations), soft delete only.
     * Unlimited users, typically ~2.
     */
    SUPERUSER,
    
    /**
     * Mini user - Limited access to files, reminders, own backups.
     * Unlimited users, typically ~2.
     */
    MINIUSER;
    
    /**
     * Returns session timeout in minutes for this role.
     */
    public int getSessionTimeoutMinutes() {
        return switch (this) {
            case ADMIN -> 15;
            case SUPERUSER -> 60;
            case MINIUSER -> 120;
        };
    }
    
    /**
     * Returns whether 2FA is required for this role.
     */
    public boolean requires2FA() {
        return this == ADMIN;
    }
    
    /**
     * Returns whether this role can perform hard deletes.
     */
    public boolean canHardDelete() {
        return this == ADMIN;
    }
    
    /**
     * Returns whether this role should be excluded from backups.
     */
    public boolean isExcludedFromBackups() {
        return this == ADMIN;
    }
    
    /**
     * Returns whether this role can restore backups.
     */
    public boolean canRestoreBackups() {
        return this == ADMIN;
    }
}
