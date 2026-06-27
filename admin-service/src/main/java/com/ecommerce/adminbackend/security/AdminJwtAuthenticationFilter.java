package com.ecommerce.adminbackend.security;

import com.ecommerce.adminbackend.logging.LogFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LogFactory.getLogger(AdminJwtAuthenticationFilter.class);

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/admin/auth/login",
            "/api/admin/health"
    );

    private final AdminJwtService adminJwtService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (!path.startsWith("/api/admin/")) {
            return true;
        }
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for {}", request.getRequestURI());
            writeUnauthorized(response, "Admin session required. Please log in.");
            return;
        }

        String token = authHeader.substring(7).trim();
        Optional<Long> adminId = adminJwtService.parseAdminId(token);
        if (adminId.isEmpty()) {
            log.warn("Invalid or expired JWT for {}", request.getRequestURI());
            writeUnauthorized(response, "Invalid or expired admin session.");
            return;
        }

        String role = adminJwtService.parseClaims(token)
                .map(claims -> claims.get("role", String.class))
                .orElse("ADMIN");

        request.setAttribute("authenticatedAdminId", adminId.get());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        adminId.get(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("message", message));
    }
}
