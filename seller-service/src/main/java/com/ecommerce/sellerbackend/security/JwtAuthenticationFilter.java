package com.ecommerce.sellerbackend.security;

import com.ecommerce.sellerbackend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String SELLER_ID_HEADER = "X-Seller-Id";

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/verify-email",
            "/api/auth/confirm-email-link",
            "/api/auth/verify-email-otp",
            "/api/auth/resend-email-otp",
            "/api/sellers/send-otp",
            "/api/sellers/verify-otp",
            "/api/sellers/register",
            "/api/public/"
    );

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return true;
        }
        if (path.startsWith("/api/admin/")) {
            return true;
        }
        if (HttpMethod.GET.matches(request.getMethod()) && path.startsWith("/api/seller/locations/")) {
            return true;
        }
        if (HttpMethod.POST.matches(request.getMethod()) && "/api/seller/locations/enrich".equals(path)) {
            return true;
        }
        if (HttpMethod.POST.matches(request.getMethod()) && "/api/seller/locations".equals(path)) {
            return true;
        }
        if (HttpMethod.POST.matches(request.getMethod()) && "/api/seller/locations/pincodes".equals(path)) {
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
            writeUnauthorized(response, "Authentication required.");
            return;
        }

        String token = authHeader.substring(7).trim();
        Optional<Long> sellerIdFromToken = jwtService.parseSellerId(token);
        if (sellerIdFromToken.isEmpty()) {
            writeUnauthorized(response, "Invalid or expired session. Please log in again.");
            return;
        }

        String sellerIdHeader = request.getHeader(SELLER_ID_HEADER);
        if (sellerIdHeader == null || sellerIdHeader.isBlank()) {
            writeForbidden(response, "Seller session header is required.");
            return;
        }
        try {
            long headerSellerId = Long.parseLong(sellerIdHeader.trim());
            if (headerSellerId != sellerIdFromToken.get()) {
                writeForbidden(response, "Session mismatch. Please log in again.");
                return;
            }
        } catch (NumberFormatException ex) {
            writeForbidden(response, "Invalid seller session.");
            return;
        }

        request.setAttribute("authenticatedSellerId", sellerIdFromToken.get());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(sellerIdFromToken.get(), null, List.of()));
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("message", message));
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("message", message));
    }
}
