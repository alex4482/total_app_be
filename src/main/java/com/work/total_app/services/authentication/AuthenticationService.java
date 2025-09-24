package com.work.total_app.services.authentication;

import com.work.total_app.models.authentication.AuthTokens;
import com.work.total_app.models.authentication.LoginRequest;
import com.work.total_app.models.authentication.RefreshTokenRequest;

public interface AuthenticationService {
    AuthTokens login(LoginRequest req);

    AuthTokens refreshToken(RefreshTokenRequest req);
}
