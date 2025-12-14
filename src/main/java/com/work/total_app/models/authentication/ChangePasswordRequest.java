package com.work.total_app.models.authentication;

public record ChangePasswordRequest(
    String oldPassword,
    String newPassword
) {}

