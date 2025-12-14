package com.work.total_app.models.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    String username,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters long")
    String password,
    
    @Email(message = "Invalid email format")
    String email
) {}

