package com.work.total_app.models.authentication;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
public class AuthTokens {
    private final String accessToken;
    private final String refreshToken;
    private final String sessionId;
}
