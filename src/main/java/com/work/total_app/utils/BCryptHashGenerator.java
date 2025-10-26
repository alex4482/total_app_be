package com.work.total_app.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes.
 * 
 * Usage:
 * 1. Run this class as a Java application
 * 2. Change the password in main() method
 * 3. Copy the generated hash to application.properties
 * 
 * Example:
 *   Password: mySecretPassword123
 *   Hash: $2
 */
public class BCryptHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        
        // CHANGE THIS PASSWORD
        String password = "your_password_here";
        
        String hash = encoder.encode(password);
        
        System.out.println("================================================");
        System.out.println("BCrypt Hash Generator");
        System.out.println("================================================");
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("================================================");
        System.out.println("\nCopy this to your application.properties:");
        System.out.println("app.auth.universal-password-hash=" + hash);
        System.out.println("\nOr set as environment variable:");
        System.out.println("UNIVERSAL_PASSWORD_HASH=" + hash);
        System.out.println("================================================");
        
        // Verify that the hash works
        boolean matches = encoder.matches(password, hash);
        System.out.println("\nVerification: " + (matches ? "✓ OK" : "✗ FAILED"));
    }
}

