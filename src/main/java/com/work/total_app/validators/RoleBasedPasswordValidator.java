package com.work.total_app.validators;

import com.work.total_app.models.user.UserRole;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates passwords based on user role requirements.
 * 
 * <p><strong>Password Requirements by Role:</strong>
 * <ul>
 *   <li><strong>ADMIN:</strong> 12+ characters, uppercase, lowercase, number, special character</li>
 *   <li><strong>SUPERUSER:</strong> 6+ characters (no hard delete capability = lower risk)</li>
 *   <li><strong>MINIUSER:</strong> 6+ characters (limited access = lower risk)</li>
 * </ul>
 */
@Component
public class RoleBasedPasswordValidator {
    
    // ADMIN password pattern: 12+ chars, uppercase, lowercase, digit, special char
    private static final Pattern ADMIN_PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,128}$"
    );
    
    // Maximum password length (prevent BCrypt DoS)
    private static final int MAX_PASSWORD_LENGTH = 128;
    
    /**
     * Validates a password for the given role.
     * 
     * @param password The password to validate
     * @param role The user role
     * @throws WeakPasswordException if password doesn't meet requirements
     */
    public void validate(String password, UserRole role) throws WeakPasswordException {
        if (password == null || password.isEmpty()) {
            throw new WeakPasswordException("Password cannot be empty");
        }
        
        // Check maximum length (prevent BCrypt DoS)
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new WeakPasswordException(
                String.format("Password too long (max %d characters)", MAX_PASSWORD_LENGTH)
            );
        }
        
        switch (role) {
            case ADMIN:
                validateAdminPassword(password);
                break;
            case SUPERUSER:
            case MINIUSER:
                validateBasicPassword(password);
                break;
        }
    }
    
    /**
     * Validates admin password (strong requirements).
     */
    private void validateAdminPassword(String password) throws WeakPasswordException {
        List<String> errors = new ArrayList<>();
        
        if (password.length() < 12) {
            errors.add("at least 12 characters");
        }
        
        if (!password.matches(".*[a-z].*")) {
            errors.add("at least one lowercase letter");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            errors.add("at least one uppercase letter");
        }
        
        if (!password.matches(".*\\d.*")) {
            errors.add("at least one digit");
        }
        
        if (!password.matches(".*[@$!%*?&].*")) {
            errors.add("at least one special character (@$!%*?&)");
        }
        
        if (!errors.isEmpty()) {
            throw new WeakPasswordException(
                "Admin password must contain: " + String.join(", ", errors)
            );
        }
    }
    
    /**
     * Validates basic password (SUPERUSER/MINIUSER - 6+ chars).
     */
    private void validateBasicPassword(String password) throws WeakPasswordException {
        if (password.length() < 6) {
            throw new WeakPasswordException("Password must be at least 6 characters long");
        }
    }
    
    /**
     * Gets password requirements description for a role.
     */
    public String getRequirementsDescription(UserRole role) {
        return switch (role) {
            case ADMIN -> 
                "Admin password: 12+ characters, must include uppercase, lowercase, digit, and special character (@$!%*?&)";
            case SUPERUSER, MINIUSER -> 
                "Password: 6+ characters";
        };
    }
    
    /**
     * Exception thrown when password is too weak.
     */
    public static class WeakPasswordException extends Exception {
        public WeakPasswordException(String message) {
            super(message);
        }
    }
}
