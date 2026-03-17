package com.smartnotes.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class RequestLoggingFilter implements Filter {

    private static final int MAX_BODY_LOG_LENGTH = 1000;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();

        // Skip logging for /static/** and /console/** paths
        if (requestURI.startsWith("/static") || requestURI.startsWith("/console")) {
            chain.doFilter(request, response);
            return;
        }

        String method = httpRequest.getMethod();
        String queryString = httpRequest.getQueryString();
        String remoteAddr = httpRequest.getRemoteAddr();
        String fullUri = queryString != null ? requestURI + "?" + queryString : requestURI;

        // Log request body for POST/PUT/PATCH
        String bodyInfo = "";
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            bodyInfo = logRequestBody(httpRequest);
        }

        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = httpResponse.getStatus();

            if (!bodyInfo.isEmpty()) {
                log.info("{} {} - {} - {}ms - body={}", method, fullUri, status, duration, bodyInfo);
            } else {
                log.info("{} {} - {} - {}ms", method, fullUri, status, duration);
            }
        }
    }

    private String logRequestBody(HttpServletRequest request) {
        try {
            // Use a ContentCachingRequestWrapper approach via re-reading is not possible
            // since the input stream can only be read once. We log what we can from
            // request parameters as a lightweight alternative.
            StringBuilder sb = new StringBuilder();
            var params = request.getParameterMap();
            if (!params.isEmpty()) {
                sb.append("params={");
                boolean first = true;
                for (var entry : params.entrySet()) {
                    if (!first) sb.append(", ");
                    first = false;
                    sb.append(entry.getKey()).append("=");
                    String[] values = entry.getValue();
                    if (values.length == 1) {
                        sb.append(truncate(values[0]));
                    } else {
                        sb.append("[");
                        for (int i = 0; i < values.length; i++) {
                            if (i > 0) sb.append(",");
                            sb.append(truncate(values[i]));
                        }
                        sb.append("]");
                    }
                }
                sb.append("}");
            }
            return sb.length() > 0 ? sb.toString() : "";
        } catch (Exception e) {
            log.debug("Failed to log request body: {}", e.getMessage());
            return "[unreadable]";
        }
    }

    private String truncate(String value) {
        if (value == null) return "null";
        if (value.length() <= MAX_BODY_LOG_LENGTH) return value;
        return value.substring(0, MAX_BODY_LOG_LENGTH) + "...[truncated]";
    }
}
