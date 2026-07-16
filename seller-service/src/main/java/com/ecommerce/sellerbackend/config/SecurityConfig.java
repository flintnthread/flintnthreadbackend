package com.ecommerce.sellerbackend.config;

import com.ecommerce.sellerbackend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins:http://localhost:8081,http://localhost:19006,http://127.0.0.1:8081}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password/**",
                                "/api/auth/verify-email",
                                "/api/auth/confirm-email-link",
                                "/api/auth/verify-email-otp",
                                "/api/auth/resend-email-otp",
                                "/api/sellers/send-otp",
                                "/api/sellers/verify-otp",
                                "/api/sellers/register",
                                "/api/public/**",
                                "/api/admin/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/seller/locations/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/seller/locations/enrich").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/seller/locations").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/seller/locations/pincodes").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = new java.util.ArrayList<>(
                java.util.Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isBlank())
                        .toList()
        );
        // Always allow both production domains (apex + subdomains) + local web.
        origins.addAll(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://flintnthread.online",
                "https://*.flintnthread.online",
                "https://flintnthread.in",
                "https://*.flintnthread.in"
        ));
        config.setAllowedOriginPatterns(origins.stream().distinct().toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
