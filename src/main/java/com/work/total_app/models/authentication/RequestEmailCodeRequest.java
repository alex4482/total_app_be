package com.work.total_app.models.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestEmailCodeRequest(
    @NotBlank(message = "Username is required")
    String username,
    
    @NotBlank(message = "Password is required")
    String password,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email
) {}

