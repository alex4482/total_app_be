package com.work.total_app.models.authentication;

import lombok.*;

@ToString
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    private AuthTokens tokens;
//    private User user; // no need yet
}
