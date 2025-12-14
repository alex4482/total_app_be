package com.work.total_app.models.authentication;

public record RegisterUserRequest(
    String username,
    String password,
    String email
) {}

