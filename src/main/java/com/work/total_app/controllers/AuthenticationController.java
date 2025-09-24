package com.work.total_app.controllers;

import com.work.total_app.constants.AuthenticationConstants;
import com.work.total_app.models.authentication.*;
import com.work.total_app.services.authentication.AuthenticationService;
import com.work.total_app.services.authentication.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/auth")
@CrossOrigin(origins = {AuthenticationConstants.PROD_WEBSITE_URL, }, originPatterns = {AuthenticationConstants.LOCAL_WEBSITE_PATTERN, AuthenticationConstants.STAGING_WEBSITE_PATTERN})
@Log4j2
public class AuthenticationController {

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody LoginRequest authRequest) {
        if (authRequest == null || authRequest.password() == null) {
            log.info("Returning forbidden for request <{}>", authRequest);
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }
        AuthTokens tokens;
        try {
            tokens = authService.login(authRequest);
        }
        catch (ResponseStatusException e)
        {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        log.info("Token is <{}>", tokens);
        if (tokens == null) {
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }
        AuthenticationResponse response = AuthenticationResponse.builder()
                .tokens(tokens)
                .build();

        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        AuthTokens newTokens = authService.refreshToken(request);
        if (newTokens == null) {
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }

        AuthenticationResponse.AuthenticationResponseBuilder response = AuthenticationResponse.builder();
        response.tokens(newTokens);

        return new ResponseEntity<>(response.build(), HttpStatus.ACCEPTED);
    }
}
