package com.work.total_app.services.security;

import com.work.total_app.models.user.EmailWhitelist;
import com.work.total_app.repositories.EmailWhitelistRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class EmailWhitelistService {
    
    @Autowired
    private EmailWhitelistRepository emailWhitelistRepository;
    
    public boolean isEmailWhitelisted(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        String normalizedEmail = email.toLowerCase().trim();
        boolean whitelisted = emailWhitelistRepository.existsByEmailAndActiveTrue(normalizedEmail);
        
        log.debug("Email whitelist check for '{}': {}", normalizedEmail, whitelisted);
        return whitelisted;
    }
    
    public void addEmailToWhitelist(String email, String description) {
        String normalizedEmail = email.toLowerCase().trim();
        
        if (emailWhitelistRepository.existsByEmailAndActiveTrue(normalizedEmail)) {
            log.warn("Email '{}' is already in whitelist", normalizedEmail);
            return;
        }
        
        EmailWhitelist whitelist = new EmailWhitelist();
        whitelist.setEmail(normalizedEmail);
        whitelist.setActive(true);
        whitelist.setDescription(description);
        
        emailWhitelistRepository.save(whitelist);
        log.info("Added email '{}' to whitelist", normalizedEmail);
    }
}

