package com.ecommerce.adminbackend.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Logs every admin API request/response with duration and status.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AdminRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LogFactory.getLogger(AdminRequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/admin/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long started = System.currentTimeMillis();
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String fullPath = query == null || query.isBlank() ? path : path + "?" + query;

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long durationMs = System.currentTimeMillis() - started;
            int status = wrappedResponse.getStatus();
            Object adminId = request.getAttribute("authenticatedAdminId");
            String adminPart = adminId != null ? " adminId=" + adminId : "";

            if (status >= 500) {
                log.error("API {} {} -> {} ({} ms){}", method, fullPath, status, durationMs, adminPart);
            } else if (status >= 400) {
                log.warn("API {} {} -> {} ({} ms){}", method, fullPath, status, durationMs, adminPart);
            } else {
                log.info("API {} {} -> {} ({} ms){}", method, fullPath, status, durationMs, adminPart);
            }

            wrappedResponse.copyBodyToResponse();
        }
    }
}
