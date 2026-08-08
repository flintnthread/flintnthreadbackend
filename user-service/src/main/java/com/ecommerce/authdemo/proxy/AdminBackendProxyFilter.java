package com.ecommerce.authdemo.proxy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Enumeration;
import java.util.Set;

/**
 * Production nginx often routes all /api/* to user-service (8080).
 * Forward /api/admin/* to admin-service (8082) on the same host.
 */
@Component
public class AdminBackendProxyFilter extends OncePerRequestFilter {

    private static final Set<String> HOP_BY_HOP = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailers", "transfer-encoding", "upgrade", "host", "content-length"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Value("${app.admin.backend-url:http://127.0.0.1:8082}")
    private String adminBackendBaseUrl;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/admin/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            proxy(request, response);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            writeError(response, HttpServletResponse.SC_BAD_GATEWAY, "Admin API proxy interrupted.");
        } catch (Exception ex) {
            writeError(response, HttpServletResponse.SC_BAD_GATEWAY,
                    "Admin API unavailable. Ensure admin-service is running on port 8082.");
        }
    }

    private void proxy(HttpServletRequest request, HttpServletResponse response)
            throws IOException, InterruptedException {
        String base = adminBackendBaseUrl.replaceAll("/+$", "");
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String targetUrl = base + path + (query == null || query.isBlank() ? "" : "?" + query);

        byte[] body = readBody(request);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(120));

        copyRequestHeaders(request, builder);
        builder.method(request.getMethod(), body.length == 0
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(body));

        HttpResponse<InputStream> upstream = httpClient.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofInputStream());

        response.setStatus(upstream.statusCode());
        upstream.headers().map().forEach((name, values) -> {
            if (shouldSkipResponseHeader(name)) {
                return;
            }
            for (String value : values) {
                response.addHeader(name, value);
            }
        });

        try (InputStream in = upstream.body(); OutputStream out = response.getOutputStream()) {
            in.transferTo(out);
        }
    }

    private static byte[] readBody(HttpServletRequest request) throws IOException {
        return request.getInputStream().readAllBytes();
    }

    private static void copyRequestHeaders(HttpServletRequest request, HttpRequest.Builder builder) {
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (shouldSkipRequestHeader(name)) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                builder.header(name, values.nextElement());
            }
        }
    }

    private static boolean shouldSkipRequestHeader(String name) {
        return name != null && HOP_BY_HOP.contains(name.toLowerCase());
    }

    private static boolean shouldSkipResponseHeader(String name) {
        return name != null && HOP_BY_HOP.contains(name.toLowerCase());
    }

    private static void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"" + message.replace("\"", "\\\"") + "\"}");
    }
}
