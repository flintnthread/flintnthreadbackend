package com.ecommerce.sellerbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Current mandal / district from OpenStreetMap — used when India Post data is outdated
 * (e.g. Guntur district before AP reorg, missing Block on postoffice search).
 */
@Service
@RequiredArgsConstructor
public class NominatimAdminLookupService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "SellerApp/1.0 (seller-address-onboarding)";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Map<String, AdminDivisions> cache = new LinkedHashMap<>();

    public record AdminDivisions(String mandal, String district) {}

    public AdminDivisions lookup(String village, String stateHint) {
        if (village == null || village.isBlank()) {
            return new AdminDivisions("", "");
        }
        String key = (village.trim() + "|" + (stateHint != null ? stateHint.trim() : "")).toLowerCase(Locale.ROOT);
        AdminDivisions cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        AdminDivisions resolved = fetchFromNominatim(village.trim(), stateHint);
        if (cache.size() > 200) {
            cache.clear();
        }
        cache.put(key, resolved);
        return resolved;
    }

    private AdminDivisions fetchFromNominatim(String village, String stateHint) {
        try {
            String query = stateHint != null && !stateHint.isBlank()
                    ? village + ", " + stateHint.trim() + ", India"
                    : village + ", India";

            URI uri = UriComponentsBuilder.fromHttpUrl(NOMINATIM_URL)
                    .queryParam("q", query)
                    .queryParam("countrycodes", "in")
                    .queryParam("format", "json")
                    .queryParam("addressdetails", "1")
                    .queryParam("limit", "1")
                    .build()
                    .encode()
                    .toUri();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept-Language", "en")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new AdminDivisions("", "");
            }

            String body = response.body();
            if (body == null || body.isBlank()) {
                return new AdminDivisions("", "");
            }

            JsonNode root = objectMapper.readTree(body);
            if (!root.isArray() || root.isEmpty()) {
                return new AdminDivisions("", "");
            }

            JsonNode address = root.get(0).path("address");
            String mandal = stripAdminSuffix(firstNonBlank(address, "county", "city", "town", "municipality"));
            String district = firstNonBlank(address, "state_district", "district");
            return new AdminDivisions(mandal, district);
        } catch (Exception ignored) {
            return new AdminDivisions("", "");
        }
    }

    private static String firstNonBlank(JsonNode address, String... fields) {
        for (String field : fields) {
            JsonNode value = address.get(field);
            if (value != null && !value.isNull()) {
                String text = value.asText("").trim();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }

    static String stripAdminSuffix(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim()
                .replaceAll("(?i)\\s+mandal$", "")
                .replaceAll("(?i)\\s+taluk$", "")
                .replaceAll("(?i)\\s+tehsil$", "")
                .replaceAll("(?i)\\s+block$", "")
                .trim();
    }

    /** Normalize India Post block spelling (Gurazalla → Gurazala). */
    static String normalizeMandalName(String mandal) {
        if (mandal == null || mandal.isBlank()) {
            return "";
        }
        String trimmed = stripAdminSuffix(mandal);
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.endsWith("zalla")) {
            return trimmed.substring(0, trimmed.length() - 1) + "a";
        }
        return trimmed;
    }
}
