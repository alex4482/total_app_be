package com.work.total_app.services.authentication;

import com.work.total_app.models.authentication.*;
import com.work.total_app.models.user.User;

public interface AuthenticationService {
    AuthTokens login(LoginRequest req, String ip, String userAgent);
    AuthTokens loginWithEmail(LoginWithEmailRequest req, String ip, String userAgent);
    boolean requestEmailVerificationCode(RequestEmailCodeRequest req, String ip);
    AuthTokens refreshToken(RefreshTokenRequest req);
    void logout(String sessionId);
    User registerUser(RegisterUserRequest req);
    void changePassword(User user, ChangePasswordRequest req);
}
