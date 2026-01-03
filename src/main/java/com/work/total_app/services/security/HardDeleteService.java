package com.work.total_app.services.security;

import com.work.total_app.models.user.User;
import com.work.total_app.models.user.UserRole;
import com.work.total_app.repositories.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for hard delete operations (ADMIN only).
 * 
 * <p>Hard delete permanently removes records from the database with no recovery option.
 * 
 * <p>Security features:
 * <ul>
 *   <li>ADMIN role required</li>
 *   <li>Email verification mandatory</li>
 *   <li>Rate limiting on verification attempts (5 fails → 30 min lockout)</li>
 *   <li>Comprehensive audit logging</li>
 * </ul>
 * 
 * <p><strong>Two-step process:</strong>
 * <ol>
 *   <li>Initiate deletion → sends verification code to admin's email</li>
 *   <li>Confirm deletion → verifies code and performs hard delete</li>
 * </ol>
 */
@Service
@Log4j2
public class HardDeleteService {
    
    @Autowired
    private HardDeleteVerificationService verificationService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Step 1: Initiates a hard delete operation by sending verification code.
     * 
     * @param admin The admin user requesting the deletion
     * @param targetId The ID of the entity to delete
     * @param entityType The type of entity (User, File, etc.)
     * @return Verification session ID
     * @throws SecurityException if user is not ADMIN or rate limited
     */
    public String initiateHardDelete(User admin, UUID targetId, String entityType) throws SecurityException {
        // Verify admin role
        if (admin.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Only ADMIN users can perform hard deletes");
        }
        
        // Build operation description
        String description = String.format(
            "Hard delete %s with ID: %s\nRequested by: %s\nThis action is IRREVERSIBLE.",
            entityType,
            targetId,
            admin.getUsername()
        );
        
        // Send verification code
        String sessionId = verificationService.initiateVerification(admin, description);
        
        log.warn("Hard delete initiated for {} {} by admin {}", entityType, targetId, admin.getUsername());
        
        return sessionId;
    }
    
    /**
     * Step 2: Confirms and executes the hard delete after email verification.
     * 
     * @param admin The admin user
     * @param sessionId The verification session ID
     * @param verificationCode The code from email
     * @param targetId The ID of the entity to delete
     * @param entityType The type of entity
     * @throws SecurityException if verification fails or user not ADMIN
     */
    @Transactional
    public void confirmHardDelete(
            User admin, 
            String sessionId, 
            String verificationCode, 
            UUID targetId, 
            String entityType
    ) throws SecurityException {
        // Verify admin role
        if (admin.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Only ADMIN users can perform hard deletes");
        }
        
        // Verify code
        boolean verified = verificationService.verifyCode(admin, sessionId, verificationCode);
        if (!verified) {
            throw new SecurityException("Email verification failed");
        }
        
        // Perform the hard delete based on entity type
        switch (entityType.toUpperCase()) {
            case "USER":
                hardDeleteUser(targetId, admin);
                break;
            case "FILE":
                // Hard delete file will be implemented in file service
                log.info("Hard delete file {} confirmed by admin {}", targetId, admin.getUsername());
                break;
            default:
                throw new IllegalArgumentException("Unknown entity type: " + entityType);
        }
        
        log.warn("HARD DELETE executed: {} {} by admin {} (verified)", 
            entityType, targetId, admin.getUsername());
    }
    
    /**
     * Hard deletes a user from the database.
     * 
     * @param userId The user ID to hard delete
     * @param deletedBy The admin performing the deletion
     */
    @Transactional
    public void hardDeleteUser(UUID userId, User deletedBy) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        
        // Prevent deleting the last ADMIN
        if (user.getRole() == UserRole.ADMIN) {
            long adminCount = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .count();
            
            if (adminCount <= 1) {
                throw new SecurityException("Cannot delete the last ADMIN user");
            }
        }
        
        String deletedUsername = user.getUsername();
        userRepository.delete(user);
        
        log.warn("User {} ({}) HARD DELETED by admin {}", 
            deletedUsername, userId, deletedBy.getUsername());
    }
    
    /**
     * Cancels an active hard delete verification session.
     * 
     * @param userId The user who initiated the deletion
     */
    public void cancelHardDelete(UUID userId) {
        verificationService.cancelVerification(userId);
        log.info("Hard delete cancelled for user {}", userId);
    }
    
    /**
     * Hard deletes a soft-deleted user (ADMIN can clean up soft-deleted records).
     * 
     * @param userId The soft-deleted user ID
     * @param admin The admin performing the hard delete
     * @param sessionId Verification session ID
     * @param verificationCode Verification code
     */
    @Transactional
    public void hardDeleteSoftDeletedUser(
            UUID userId, 
            User admin, 
            String sessionId, 
            String verificationCode
    ) throws SecurityException {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        
        if (!user.isDeleted()) {
            throw new IllegalStateException("User is not soft-deleted. Use normal hard delete flow.");
        }
        
        // Verify admin and code
        confirmHardDelete(admin, sessionId, verificationCode, userId, "USER");
    }
}
