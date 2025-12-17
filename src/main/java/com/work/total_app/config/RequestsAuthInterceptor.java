package com.work.total_app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.work.total_app.utils.TokenVerifier;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestsAuthInterceptor extends OncePerRequestFilter {
        private final List<TokenVerifier> authenticators;
        private final BearerTokenResolver resolver = new DefaultBearerTokenResolver();
        private final List<String> allowed = List.of("/auth/login", "/auth/refresh-token");
        private final ObjectMapper objectMapper = new ObjectMapper();

        public RequestsAuthInterceptor(List<TokenVerifier> authenticators) {
            this.authenticators = authenticators;
        }

        @Override
        protected boolean shouldNotFilter(@NonNull HttpServletRequest req) {
            String p = req.getRequestURI();
            return "OPTIONS".equalsIgnoreCase(req.getMethod()) || allowed.contains(p);
        }

        @Override
        protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
      throws ServletException, IOException {

            String token = resolver.resolve(request);
            if (token != null) {
                Authentication auth = null;
                AuthenticationException lastErr = null;
                boolean isTokenExpired = false;

                for (TokenVerifier a : authenticators) {
                    try {
                        auth = a.verifyToken(token);
                        if (auth != null) break;              // success
                    } catch (AuthenticationException e) {
                        lastErr = e;
                        // Check if it's a token expiration issue
                        if (e.getCause() instanceof ExpiredJwtException) {
                            isTokenExpired = true;
                        }
                        // continue trying next provider (lets you support multiple)
                    }
                }

                if (auth != null) {
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else if (lastErr != null) {
                    // Return detailed error response for better frontend handling
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", isTokenExpired ? "TOKEN_EXPIRED" : "INVALID_TOKEN");
                    errorResponse.put("message", isTokenExpired 
                        ? "Your session has expired. Please log in again." 
                        : "Invalid or malformed token.");
                    errorResponse.put("status", 401);
                    errorResponse.put("timestamp", Instant.now().toString());
                    errorResponse.put("requiresLogin", true);
                    
                    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                    return;
                }
            }

            chain.doFilter(request, response);
        }
    }