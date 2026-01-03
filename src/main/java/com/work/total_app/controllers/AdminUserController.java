package com.work.total_app.controllers;

import com.work.total_app.models.api.ApiResponse;
import com.work.total_app.models.user.User;
import com.work.total_app.models.user.UserRole;
import com.work.total_app.repositories.UserRepository;
import com.work.total_app.services.security.HardDeleteService;
import com.work.total_app.services.security.SoftDeleteService;
import com.work.total_app.validators.RoleBasedPasswordValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Admin controller for user management.
 * 
 * <p>All endpoints require ADMIN role.
 * 
 * <p>Features:
 * <ul>
 *   <li>Create users with specific roles</li>
 *   <li>Update user details (username, email, role)</li>
 *   <li>Change user passwords</li>
 *   <li>Soft delete users (SUPERUSER/MINIUSER only)</li>
 *   <li>Hard delete users (with email verification)</li>
 *   <li>Restore soft-deleted users</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Log4j2
public class AdminUserController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private RoleBasedPasswordValidator passwordValidator;
    
    @Autowired
    private SoftDeleteService softDeleteService;
    
    @Autowired
    private HardDeleteService hardDeleteService;
    
    /**
     * Creates a new user with specified role.
     * 
     * POST /admin/users
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @AuthenticationPrincipal com.work.total_app.models.user.UserPrincipal principal,
            @Valid @RequestBody CreateUserRequest request
    ) {
        try {
            // Get admin user
            User admin = userRepository.findById(principal.userId())
                .orElseThrow(() -> new SecurityException("Admin user not found"));
            
            // Validate username uniqueness
            if (userRepository.findByUsername(request.username).isPresent()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Username already exists")
                );
            }
            
            // Validate email uniqueness (if provided)
            if (request.email != null && !request.email.isBlank()) {
                if (userRepository.findByEmail(request.email).isPresent()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error("Email already exists")
                    );
                }
            }
            
            // Validate password for role
            try {
                passwordValidator.validate(request.password, request.role);
            } catch (RoleBasedPasswordValidator.WeakPasswordException e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
                );
            }
            
            // Prevent creating multiple ADMIN users
            if (request.role == UserRole.ADMIN) {
                long adminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == UserRole.ADMIN)
                    .count();
                
                if (adminCount >= 1) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error("Only one ADMIN user allowed in the system")
                    );
                }
            }
            
            // Create user
            User user = new User();
            user.setUsername(request.username);
            user.setEmail(request.email);
            user.setPasswordHash(passwordEncoder.encode(request.password));
            user.setRole(request.role);
            user.setEnabled(true);
            
            userRepository.save(user);
            
            log.info("User {} created with role {} by admin {}", 
                user.getUsername(), user.getRole(), admin.getUsername());
            
            return ResponseEntity.ok(ApiResponse.success(
                "User created successfully",
                toDto(user)
            ));
            
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to create user: " + e.getMessage())
            );
        }
    }
    
    /**
     * Updates an existing user.
     * 
     * PUT /admin/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @AuthenticationPrincipal com.work.total_app.models.user.UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        try {
            User admin = userRepository.findById(principal.userId())
                .orElseThrow(() -> new SecurityException("Admin user not found"));
            
            User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Update username if provided and different
            if (request.username != null && !request.username.equals(user.getUsername())) {
                if (userRepository.findByUsername(request.username).isPresent()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error("Username already exists")
                    );
                }
                user.setUsername(request.username);
            }
            
            // Update email if provided
            if (request.email != null && !request.email.equals(user.getEmail())) {
                if (userRepository.findByEmail(request.email).isPresent()) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error("Email already exists")
                    );
                }
                user.setEmail(request.email);
            }
            
            // Update role if provided
            if (request.role != null && request.role != user.getRole()) {
                // Prevent changing ADMIN role if it's the last admin
                if (user.getRole() == UserRole.ADMIN) {
                    long adminCount = userRepository.findAll().stream()
                        .filter(u -> u.getRole() == UserRole.ADMIN)
                        .count();
                    
                    if (adminCount <= 1) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error("Cannot change role of the last ADMIN user")
                        );
                    }
                }
                
                // Prevent creating multiple ADMINs
                if (request.role == UserRole.ADMIN) {
                    long adminCount = userRepository.findAll().stream()
                        .filter(u -> u.getRole() == UserRole.ADMIN && !u.getId().equals(id))
                        .count();
                    
                    if (adminCount >= 1) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error("Only one ADMIN user allowed in the system")
                        );
                    }
                }
                
                user.setRole(request.role);
            }
            
            userRepository.save(user);
            
            log.info("User {} updated by admin {}", user.getUsername(), admin.getUsername());
            
            return ResponseEntity.ok(ApiResponse.success(
                "User updated successfully",
                toDto(user)
            ));
            
        } catch (Exception e) {
            log.error("Error updating user", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to update user: " + e.getMessage())
            );
        }
    }
    
    /**
     * Changes a user's password.
     * 
     * PUT /admin/users/{id}/password
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<String>> changeUserPassword(
            @AuthenticationPrincipal com.work.total_app.models.user.UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        try {
            User admin = userRepository.findById(principal.userId())
                .orElseThrow(() -> new SecurityException("Admin user not found"));
            
            User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            // Validate password for role
            try {
                passwordValidator.validate(request.newPassword, user.getRole());
            } catch (RoleBasedPasswordValidator.WeakPasswordException e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
                );
            }
            
            user.setPasswordHash(passwordEncoder.encode(request.newPassword));
            userRepository.save(user);
            
            log.info("Password changed for user {} by admin {}", user.getUsername(), admin.getUsername());
            
            return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
            
        } catch (Exception e) {
            log.error("Error changing password", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to change password: " + e.getMessage())
            );
        }
    }
    
    /**
     * Step 1: Initiates hard delete (sends verification email).
     * 
     * POST /admin/users/{id}/hard-delete/initiate
     */
    @PostMapping("/{id}/hard-delete/initiate")
    public ResponseEntity<ApiResponse<Map<String, String>>> initiateHardDelete(
            @AuthenticationPrincipal com.work.total_app.models.user.UserPrincipal principal,
            @PathVariable UUID id
    ) {
        try {
            User admin = userRepository.findById(principal.userId())
                .orElseThrow(() -> new SecurityException("Admin user not found"));
            
            String sessionId = hardDeleteService.initiateHardDelete(admin, id, "USER");
            
            Map<String, String> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("message", "Verification code sent to your email");
            
            return ResponseEntity.ok(ApiResponse.success(
                "Hard delete initiated. Check your email for verification code.",
                response
            ));
            
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(
                ApiResponse.error(e.getMessage())
            );
        } catch (Exception e) {
            log.error("Error initiating hard delete", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to initiate hard delete: " + e.getMessage())
            );
        }
    }
    
    /**
     * Step 2: Confirms and executes hard delete.
     * 
     * DELETE /admin/users/{id}/hard-delete/confirm
     */
    @DeleteMapping("/{id}/hard-delete/confirm")
    public ResponseEntity<ApiResponse<String>> confirmHardDelete(
            @AuthenticationPrincipal com.work.total_app.models.user.UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmHardDeleteRequest request
    ) {
        try {
            User admin = userRepository.findById(principal.userId())
                .orElseThrow(() -> new SecurityException("Admin user not found"));
            
            hardDeleteService.confirmHardDelete(
                admin, 
                request.sessionId, 
                request.verificationCode, 
                id, 
                "USER"
            );
            
            return ResponseEntity.ok(ApiResponse.success("User permanently deleted", null));
            
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(
                ApiResponse.error(e.getMessage())
            );
        } catch (Exception e) {
            log.error("Error confirming hard delete", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to confirm hard delete: " + e.getMessage())
            );
        }
    }
    
    /**
     * Soft deletes a user (SUPERUSER/MINIUSER only).
     * 
     * DELETE /admin/users/{id}/soft
     */
    @DeleteMapping("/{id}/soft")
    public ResponseEntity<ApiResponse<String>> softDeleteUser(
            @AuthenticationPrincipal com.work.total_app.models.user.UserPrincipal principal,
            @PathVariable UUID id
    ) {
        try {
            User admin = userRepository.findById(principal.userId())
                .orElseThrow(() -> new SecurityException("Admin user not found"));
            
            softDeleteService.softDeleteUser(id, admin);
            
            return ResponseEntity.ok(ApiResponse.success("User soft deleted successfully", null));
            
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(
                ApiResponse.error(e.getMessage())
            );
        } catch (Exception e) {
            log.error("Error soft deleting user", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to soft delete user: " + e.getMessage())
            );
        }
    }
    
    /**
     * Restores a soft-deleted user.
     * 
     * POST /admin/users/{id}/restore
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<UserDto>> restoreUser(
            @AuthenticationPrincipal com.work.total_app.models.user.UserPrincipal principal,
            @PathVariable UUID id
    ) {
        try {
            User admin = userRepository.findById(principal.userId())
                .orElseThrow(() -> new SecurityException("Admin user not found"));
            
            softDeleteService.restoreUser(id, admin);
            
            User user = userRepository.findById(id).orElseThrow();
            
            return ResponseEntity.ok(ApiResponse.success(
                "User restored successfully",
                toDto(user)
            ));
            
        } catch (Exception e) {
            log.error("Error restoring user", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to restore user: " + e.getMessage())
            );
        }
    }
    
    // DTOs
    
    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "Username is required")
        private String username;
        
        @Email(message = "Invalid email format")
        private String email;
        
        @NotBlank(message = "Password is required")
        private String password;
        
        private UserRole role = UserRole.SUPERUSER;
    }
    
    @Data
    public static class UpdateUserRequest {
        private String username;
        private String email;
        private UserRole role;
    }
    
    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "New password is required")
        private String newPassword;
    }
    
    @Data
    public static class ConfirmHardDeleteRequest {
        @NotBlank(message = "Session ID is required")
        private String sessionId;
        
        @NotBlank(message = "Verification code is required")
        private String verificationCode;
    }
    
    @Data
    public static class UserDto {
        private String id;
        private String username;
        private String email;
        private String role;
        private boolean enabled;
        private boolean deleted;
        private String createdAt;
    }
    
    private UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId().toString());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setEnabled(user.isEnabled());
        dto.setDeleted(user.isDeleted());
        dto.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        return dto;
    }
}
