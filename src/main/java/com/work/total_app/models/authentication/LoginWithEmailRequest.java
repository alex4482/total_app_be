package com.work.total_app.models.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginWithEmailRequest(
    @NotBlank(message = "Username is required")
    String username,
    
    @NotBlank(message = "Password is required")
    String password,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Verification code must be 6 digits")
    String verificationCode
) {}

