package com.work.total_app.models.authentication;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AuthTokens {
    private final String idToken;
    private final String refreshToken;
    private final String sessionId;
}
