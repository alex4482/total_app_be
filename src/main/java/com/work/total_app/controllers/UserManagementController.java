package com.work.total_app.controllers;

import com.work.total_app.models.user.User;
import com.work.total_app.repositories.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@Log4j2
public class UserManagementController {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get all users
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        // Remove password hashes from response
        users.forEach(user -> user.setPasswordHash("[REDACTED]"));
        return ResponseEntity.ok(users);
    }
    
    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        return userRepository.findById(id)
            .map(user -> {
                user.setPasswordHash("[REDACTED]");
                return ResponseEntity.ok(user);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Enable/disable user
     */
    @PutMapping("/{id}/enabled")
    public ResponseEntity<Map<String, String>> setUserEnabled(
            @PathVariable UUID id,
            @RequestParam boolean enabled) {
        
        return userRepository.findById(id)
            .map(user -> {
                user.setEnabled(enabled);
                userRepository.save(user);
                String message = enabled ? "User enabled" : "User disabled";
                log.info("{} for user: {}", message, user.getUsername());
                return ResponseEntity.ok(Map.of("message", message));
            })
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "User not found")));
    }
    
    /**
     * Unlock user account (reset failed attempts and unlock)
     */
    @PostMapping("/{id}/unlock")
    public ResponseEntity<Map<String, String>> unlockUser(@PathVariable UUID id) {
        return userRepository.findById(id)
            .map(user -> {
                user.setAccountLocked(false);
                user.setAccountLockedUntil(null);
                user.setFailedLoginAttempts(0);
                user.setRequiresEmailVerification(false);
                user.setLastFailedLoginAt(null);
                userRepository.save(user);
                log.info("User unlocked: {}", user.getUsername());
                return ResponseEntity.ok(Map.of("message", "User unlocked successfully"));
            })
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "User not found")));
    }
    
    /**
     * Reset user's email verification requirement
     */
    @PostMapping("/{id}/reset-email-verification")
    public ResponseEntity<Map<String, String>> resetEmailVerification(@PathVariable UUID id) {
        return userRepository.findById(id)
            .map(user -> {
                user.setRequiresEmailVerification(false);
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
                log.info("Email verification reset for user: {}", user.getUsername());
                return ResponseEntity.ok(Map.of("message", "Email verification requirement reset"));
            })
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "User not found")));
    }
    
    /**
     * Delete user
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable UUID id) {
        return userRepository.findById(id)
            .map(user -> {
                String username = user.getUsername();
                userRepository.delete(user);
                log.warn("User deleted: {}", username);
                return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
            })
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "User not found")));
    }
    
    /**
     * Get user statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        long totalUsers = userRepository.count();
        long enabledUsers = userRepository.findAll().stream()
            .filter(User::isEnabled)
            .count();
        long lockedUsers = userRepository.findAll().stream()
            .filter(User::isAccountLocked)
            .count();
        long requiresEmailVerification = userRepository.findAll().stream()
            .filter(User::isRequiresEmailVerification)
            .count();
        
        return ResponseEntity.ok(Map.of(
            "totalUsers", totalUsers,
            "enabledUsers", enabledUsers,
            "lockedUsers", lockedUsers,
            "requiresEmailVerification", requiresEmailVerification
        ));
    }
}

