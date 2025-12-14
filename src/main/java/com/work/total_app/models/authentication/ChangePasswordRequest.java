package com.work.total_app.models.authentication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "Old password is required")
    String oldPassword,
    
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be at least 8 characters long")
    String newPassword
) {}

