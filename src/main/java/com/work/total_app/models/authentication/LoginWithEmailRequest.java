package com.work.total_app.models.authentication;

public record LoginWithEmailRequest(
    String username,
    String password,
    String email,
    String verificationCode
) {}

