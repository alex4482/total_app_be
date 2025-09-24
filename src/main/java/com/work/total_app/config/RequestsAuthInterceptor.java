package com.work.total_app.config;

import com.work.total_app.utils.TokenVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class RequestsAuthInterceptor extends OncePerRequestFilter {
        private final List<TokenVerifier> authenticators;
        private final BearerTokenResolver resolver = new DefaultBearerTokenResolver();
        private final List<String> allowed = List.of("/auth/login", "/auth/refresh");

        public RequestsAuthInterceptor(List<TokenVerifier> authenticators) {
            this.authenticators = authenticators;
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest req) {
            String p = req.getRequestURI();
            return "OPTIONS".equalsIgnoreCase(req.getMethod()) || allowed.contains(p);
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

            String token = resolver.resolve(request);
            if (token != null) {
                Authentication auth = null;
                AuthenticationException lastErr = null;

                for (TokenVerifier a : authenticators) {
                    try {
                        auth = a.verifyToken(token);
                        if (auth != null) break;              // success
                    } catch (AuthenticationException e) {
                        lastErr = e;                           // keep last error
                        // continue trying next provider (lets you support multiple)
                    }
                }

                if (auth != null) {
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else if (lastErr != null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }

            chain.doFilter(request, response);
        }
    }