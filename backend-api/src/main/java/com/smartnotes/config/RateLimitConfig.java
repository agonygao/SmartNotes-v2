package com.smartnotes.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component("rateLimitFilter")
public class RateLimitConfig implements Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final long WINDOW_MILLIS = 60_000L;

    private final ConcurrentHashMap<String, RateBucket> buckets = new ConcurrentHashMap<>();

    public static class RateBucket {
        private volatile long timestamp;
        private final AtomicInteger count;

        public RateBucket() {
            this.timestamp = System.currentTimeMillis();
            this.count = new AtomicInteger(0);
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public AtomicInteger getCount() {
            return count;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String key = resolveKey(httpRequest);
        RateBucket bucket = buckets.compute(key, (k, existing) -> {
            if (existing == null || isWindowExpired(existing)) {
                return new RateBucket();
            }
            return existing;
        });

        int currentCount = bucket.getCount().incrementAndGet();
        int remaining = Math.max(0, MAX_REQUESTS_PER_MINUTE - currentCount);

        httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        long resetTime = bucket.getTimestamp() + WINDOW_MILLIS;
        httpResponse.setHeader("X-RateLimit-Reset", String.valueOf(resetTime / 1000));

        if (currentCount > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for key: {}", key);
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"code\":429,\"message\":\"Too Many Requests\",\"data\":null}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() != null) {
            return "user:" + authentication.getPrincipal();
        }
        String ip = request.getRemoteAddr();
        if (ip == null || ip.isBlank()) {
            ip = "unknown";
        }
        return "ip:" + ip;
    }

    private boolean isWindowExpired(RateBucket bucket) {
        return System.currentTimeMillis() - bucket.getTimestamp() > WINDOW_MILLIS;
    }
}
