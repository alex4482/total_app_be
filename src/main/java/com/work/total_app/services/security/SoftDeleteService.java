package com.work.total_app.services.security;

import com.work.total_app.models.user.User;
import com.work.total_app.repositories.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for soft delete operations (non-ADMIN users).
 * 
 * <p>Soft delete marks records as deleted without actually removing them from the database.
 * This allows for data recovery and audit trails.
 * 
 * <p>Used by SUPERUSER and MINIUSER roles.
 */
@Service
@Log4j2
public class SoftDeleteService {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Soft deletes a user.
     * 
     * @param userId The user ID to soft delete
     * @param deletedBy The user performing the deletion
     * @throws IllegalStateException if user not found or already deleted
     * @throws SecurityException if trying to delete ADMIN user
     */
    @Transactional
    public void softDeleteUser(UUID userId, User deletedBy) throws IllegalStateException, SecurityException {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        
        if (user.isDeleted()) {
            throw new IllegalStateException("User is already deleted");
        }
        
        // ADMIN users cannot be soft deleted
        if (user.getRole() == com.work.total_app.models.user.UserRole.ADMIN) {
            throw new SecurityException("ADMIN users can only be hard deleted by another ADMIN");
        }
        
        user.softDelete();
        userRepository.save(user);
        
        log.info("User {} soft deleted by {}", userId, deletedBy.getUsername());
    }
    
    /**
     * Restores a soft-deleted user.
     * 
     * @param userId The user ID to restore
     * @param restoredBy The user performing the restoration
     * @throws IllegalStateException if user not found or not deleted
     */
    @Transactional
    public void restoreUser(UUID userId, User restoredBy) throws IllegalStateException {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        
        if (!user.isDeleted()) {
            throw new IllegalStateException("User is not deleted");
        }
        
        user.restore();
        userRepository.save(user);
        
        log.info("User {} restored by {}", userId, restoredBy.getUsername());
    }
    
    /**
     * Lists all soft-deleted users.
     */
    @Transactional(readOnly = true)
    public List<User> findAllDeleted() {
        return userRepository.findAll().stream()
            .filter(User::isDeleted)
            .toList();
    }
    
    /**
     * Checks if a user is soft deleted.
     */
    @Transactional(readOnly = true)
    public boolean isDeleted(UUID userId) {
        return userRepository.findById(userId)
            .map(User::isDeleted)
            .orElse(false);
    }
    
    /**
     * Gets deletion timestamp for a user.
     */
    @Transactional(readOnly = true)
    public Instant getDeletionTimestamp(UUID userId) {
        return userRepository.findById(userId)
            .map(User::getDeletedAt)
            .orElse(null);
    }
}
