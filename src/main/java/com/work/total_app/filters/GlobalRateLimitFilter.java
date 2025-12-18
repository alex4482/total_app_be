package com.work.total_app.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Global rate limiting filter to prevent DoS attacks on all endpoints.
 * 
 * Features:
 * - Per-IP rate limiting with sliding window
 * - Configurable request limits and time windows
 * - Automatic cleanup of expired entries
 * - HTTP 429 Too Many Requests response
 * 
 * Note: This is a basic in-memory implementation. For distributed systems,
 * consider using Redis or a dedicated rate limiting service.
 */
@Component
@Order(1) // Execute early in the filter chain
@Log4j2
public class GlobalRateLimitFilter implements Filter {

    // Configurable properties
    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.rate-limit.max-requests:100}")
    private int maxRequests;

    @Value("${app.rate-limit.window-seconds:60}")
    private int windowSeconds;

    @Value("${app.rate-limit.cleanup-interval-seconds:300}")
    private int cleanupIntervalSeconds;

    // Store request counts per IP: IP -> RequestWindow
    private final ConcurrentMap<String, RequestWindow> requestCounts = new ConcurrentHashMap<>();
    
    // Last cleanup timestamp
    private volatile long lastCleanupTime = System.currentTimeMillis();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip rate limiting for health checks and OPTIONS (CORS preflight)
        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        if (shouldSkipRateLimit(path, method)) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(httpRequest);

        // Check rate limit
        if (isRateLimited(clientIp)) {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            httpResponse.setStatus(429); // Too Many Requests
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                    String.format("{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Maximum %d requests per %d seconds allowed. Please try again later.\"}",
                            maxRequests, windowSeconds));
            return;
        }

        // Increment request count
        incrementRequestCount(clientIp);

        // Periodic cleanup of expired entries
        cleanupExpiredEntries();

        // Continue with the request
        chain.doFilter(request, response);
    }

    /**
     * Checks if the given IP has exceeded the rate limit.
     */
    private boolean isRateLimited(String ip) {
        RequestWindow window = requestCounts.get(ip);
        if (window == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        long windowStartTime = now - (windowSeconds * 1000L);

        // Remove expired requests from the window
        window.requests.removeIf(timestamp -> timestamp < windowStartTime);

        // Check if limit exceeded
        return window.requests.size() >= maxRequests;
    }

    /**
     * Increments the request count for the given IP.
     */
    private void incrementRequestCount(String ip) {
        long now = System.currentTimeMillis();
        requestCounts.computeIfAbsent(ip, k -> new RequestWindow()).requests.add(now);
    }

    /**
     * Periodically cleans up expired entries to prevent memory leaks.
     */
    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < cleanupIntervalSeconds * 1000L) {
            return; // Not time for cleanup yet
        }

        lastCleanupTime = now;
        long windowStartTime = now - (windowSeconds * 1000L);

        // Remove IPs with no recent requests
        requestCounts.entrySet().removeIf(entry -> {
            RequestWindow window = entry.getValue();
            window.requests.removeIf(timestamp -> timestamp < windowStartTime);
            return window.requests.isEmpty();
        });

        log.debug("Rate limit cleanup: {} IPs tracked", requestCounts.size());
    }

    /**
     * Determines if rate limiting should be skipped for this request.
     */
    private boolean shouldSkipRateLimit(String path, String method) {
        // Skip OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // Skip health check endpoints
        if (path.startsWith("/health") || path.equals("/actuator/health")) {
            return true;
        }

        // Note: Authentication endpoints have their own specialized rate limiting
        // (LoginRateLimitService, LoginDelayService, IpBlacklistService)
        // so we don't skip them here - they get both layers of protection

        return false;
    }

    /**
     * Extracts the client IP address from the request.
     * Handles X-Forwarded-For header for proxied requests.
     */
    private String getClientIp(HttpServletRequest request) {
        // Check common proxy headers first
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            int commaIndex = ip.indexOf(',');
            if (commaIndex > 0) {
                ip = ip.substring(0, commaIndex).trim();
            }
            return ip;
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }

    /**
     * Holds request timestamps for a sliding window.
     */
    private static class RequestWindow {
        // Using ConcurrentHashMap's newKeySet() for thread-safe set
        private final java.util.Set<Long> requests = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("GlobalRateLimitFilter initialized: enabled={}, maxRequests={}, windowSeconds={}",
                enabled, maxRequests, windowSeconds);
    }

    @Override
    public void destroy() {
        requestCounts.clear();
        log.info("GlobalRateLimitFilter destroyed");
    }
}
