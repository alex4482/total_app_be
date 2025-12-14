package com.work.total_app.models.authentication;

public record RequestEmailCodeRequest(
    String username,
    String password,
    String email
) {}

