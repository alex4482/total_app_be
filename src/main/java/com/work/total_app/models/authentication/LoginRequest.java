package com.work.total_app.models.authentication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    String username,
    
    @NotBlank(message = "Password is required")
    String password
) {
}
