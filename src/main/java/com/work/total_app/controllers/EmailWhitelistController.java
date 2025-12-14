package com.work.total_app.controllers;

import com.work.total_app.models.user.EmailWhitelist;
import com.work.total_app.repositories.EmailWhitelistRepository;
import com.work.total_app.services.security.EmailWhitelistService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/email-whitelist")
@Log4j2
public class EmailWhitelistController {
    
    @Autowired
    private EmailWhitelistRepository emailWhitelistRepository;
    
    @Autowired
    private EmailWhitelistService emailWhitelistService;
    
    /**
     * Get all whitelisted emails
     */
    @GetMapping
    public ResponseEntity<List<EmailWhitelist>> getAllWhitelistedEmails() {
        List<EmailWhitelist> emails = emailWhitelistRepository.findAll();
        return ResponseEntity.ok(emails);
    }
    
    /**
     * Get active whitelisted emails only
     */
    @GetMapping("/active")
    public ResponseEntity<List<EmailWhitelist>> getActiveWhitelistedEmails() {
        List<EmailWhitelist> emails = emailWhitelistRepository.findAll()
            .stream()
            .filter(EmailWhitelist::isActive)
            .toList();
        return ResponseEntity.ok(emails);
    }
    
    /**
     * Add email to whitelist
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> addEmailToWhitelist(
            @RequestBody Map<String, String> request) {
        
        String email = request.get("email");
        String description = request.get("description");
        
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Email is required"));
        }
        
        try {
            emailWhitelistService.addEmailToWhitelist(email, description);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Email added to whitelist successfully"));
        } catch (Exception e) {
            log.error("Error adding email to whitelist: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to add email to whitelist"));
        }
    }
    
    /**
     * Remove email from whitelist (soft delete by setting active=false)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> removeEmailFromWhitelist(
            @PathVariable UUID id) {
        
        return emailWhitelistRepository.findById(id)
            .map(whitelist -> {
                whitelist.setActive(false);
                emailWhitelistRepository.save(whitelist);
                return ResponseEntity.ok(Map.of("message", "Email removed from whitelist"));
            })
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Email not found")));
    }
    
    /**
     * Reactivate email in whitelist
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<Map<String, String>> activateEmail(@PathVariable UUID id) {
        return emailWhitelistRepository.findById(id)
            .map(whitelist -> {
                whitelist.setActive(true);
                emailWhitelistRepository.save(whitelist);
                return ResponseEntity.ok(Map.of("message", "Email activated"));
            })
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Email not found")));
    }
    
    /**
     * Check if email is whitelisted
     */
    @GetMapping("/check/{email}")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@PathVariable String email) {
        boolean whitelisted = emailWhitelistService.isEmailWhitelisted(email);
        return ResponseEntity.ok(Map.of("whitelisted", whitelisted));
    }
}

