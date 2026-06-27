package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.LocationSearchResponse;
import com.ecommerce.sellerbackend.util.LocationFuzzyMatcher;
import com.ecommerce.sellerbackend.util.LocationSearchNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * OpenStreetMap Nominatim geocoding for Maps-style address suggestions across India.
 */
@Service
@RequiredArgsConstructor
public class LocationGeocodeService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "SellerApp/1.0 (seller-address-onboarding)";
    private static final Pattern PINCODE = Pattern.compile("^\\d{6}$");

    private final ObjectMapper objectMapper;
    private final IndiaPincodeLookupService indiaPincodeLookupService;
    private final RestClient restClient = RestClient.create();

    public List<LocationSearchResponse> searchIndia(String query) {
        if (query == null || query.isBlank() || query.trim().length() < 2) {
            return List.of();
        }
        if (query.chars().allMatch(Character::isDigit)) {
            return List.of();
        }

        String trimmed = query.trim();
        LinkedHashMap<String, LocationSearchResponse> merged = new LinkedHashMap<>();
        for (LocationSearchResponse item : doNominatimSearch(trimmed)) {
            putGeoResult(merged, item);
        }
        if (merged.size() < 3) {
            for (String term : LocationSearchNormalizer.expandTerms(trimmed)) {
                if (term.equalsIgnoreCase(trimmed)) {
                    continue;
                }
                for (LocationSearchResponse item : doNominatimSearch(term)) {
                    putGeoResult(merged, item);
                }
                if (merged.size() >= 15) {
                    break;
                }
            }
        }
        List<LocationSearchResponse> results = new ArrayList<>(merged.values());
        results.sort((a, b) -> Double.compare(
                LocationFuzzyMatcher.bestMatchScore(trimmed, b.getArea(), b.getCity(), b.getState(), b.getDisplayName()),
                LocationFuzzyMatcher.bestMatchScore(trimmed, a.getArea(), a.getCity(), a.getState(), a.getDisplayName())
        ));
        return results.subList(0, Math.min(15, results.size()));
    }

    private void putGeoResult(LinkedHashMap<String, LocationSearchResponse> merged, LocationSearchResponse item) {
        if (item == null) {
            return;
        }
        String key = (item.getCity() + "|" + item.getState() + "|" + item.getArea() + "|" + item.getPincode())
                .toLowerCase(Locale.ROOT);
        merged.putIfAbsent(key, item);
    }

    private List<LocationSearchResponse> doNominatimSearch(String query) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(NOMINATIM_URL)
                    .queryParam("q", query.trim())
                    .queryParam("countrycodes", "in")
                    .queryParam("format", "json")
                    .queryParam("addressdetails", "1")
                    .queryParam("limit", "15")
                    .build()
                    .encode()
                    .toUri();

            String body = restClient.get()
                    .uri(uri)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept-Language", "en")
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(body);
            if (!root.isArray()) {
                return List.of();
            }

            LinkedHashMap<String, LocationSearchResponse> deduped = new LinkedHashMap<>();
            for (JsonNode node : root) {
                LocationSearchResponse item = mapNode(node);
                if (item == null) {
                    continue;
                }
                String key = (item.getCity() + "|" + item.getState() + "|" + item.getArea() + "|" + item.getPincode())
                        .toLowerCase(Locale.ROOT);
                deduped.putIfAbsent(key, item);
            }
            return new ArrayList<>(deduped.values());
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Fill missing pincode when user selects an external/OSM suggestion. */
    public LocationSearchResponse enrichPincode(LocationSearchResponse item) {
        if (item == null) {
            return item;
        }
        String existing = item.getPincode() != null ? item.getPincode().trim() : "";
        if (PINCODE.matcher(existing).matches()) {
            return item;
        }

        String district = extractDistrictFromDisplayName(item.getDisplayName());
        String mandal = item.getCity();
        if (mandal == null || mandal.isBlank()) {
            mandal = extractMandalFromDisplayName(item.getDisplayName());
        }
        String pincode = indiaPincodeLookupService.resolvePincode(
                item.getArea(),
                mandal,
                district,
                item.getState()
        );
        if (pincode.isBlank()) {
            pincode = lookupPincodeFromNominatim(item.getArea(), item.getCity(), item.getState());
        }
        if (pincode.isBlank()) {
            return item;
        }

        return LocationSearchResponse.builder()
                .externalId(item.getExternalId())
                .displayName(item.getDisplayName())
                .external(item.getExternal() != null ? item.getExternal() : Boolean.FALSE)
                .pincodeId(item.getPincodeId() != null ? item.getPincodeId() : 0)
                .pincode(pincode)
                .areaId(item.getAreaId() != null ? item.getAreaId() : 0)
                .area(item.getArea())
                .cityId(item.getCityId() != null ? item.getCityId() : 0)
                .city(item.getCity())
                .stateId(item.getStateId() != null ? item.getStateId() : 0)
                .state(item.getState())
                .countryId(item.getCountryId() != null ? item.getCountryId() : 0)
                .country(item.getCountry())
                .build();
    }

    /** Nominatim search for postcode — mirrors Google Chrome village lookup. */
    public String lookupPincodeFromNominatim(String area, String mandal, String state) {
        if (area == null || area.isBlank()) {
            return "";
        }
        try {
            String query = String.join(", ",
                    java.util.stream.Stream.of(area, mandal, state, "India")
                            .filter(s -> s != null && !s.isBlank())
                            .toList());

            URI uri = UriComponentsBuilder.fromHttpUrl(NOMINATIM_URL)
                    .queryParam("q", query)
                    .queryParam("countrycodes", "in")
                    .queryParam("format", "json")
                    .queryParam("addressdetails", "1")
                    .queryParam("limit", "3")
                    .build()
                    .encode()
                    .toUri();

            String body = restClient.get()
                    .uri(uri)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept-Language", "en")
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) {
                return "";
            }

            JsonNode root = objectMapper.readTree(body);
            if (!root.isArray()) {
                return "";
            }

            for (JsonNode node : root) {
                String postcode = firstNonBlank(node.path("address"), "postcode");
                if (PINCODE.matcher(postcode).matches()) {
                    return postcode;
                }
            }
        } catch (Exception ignored) {
            // best-effort
        }
        return "";
    }

    private static String extractDistrictFromDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "";
        }
        String[] parts = displayName.split(",");
        if (parts.length >= 3) {
            return parts[2].trim();
        }
        return "";
    }

    private static String extractMandalFromDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "";
        }
        String[] parts = displayName.split(",");
        if (parts.length >= 2) {
            return parts[1].trim();
        }
        return "";
    }

    private LocationSearchResponse mapNode(JsonNode node) {
        JsonNode address = node.get("address");
        if (address == null || address.isNull()) {
            return null;
        }

        String country = firstNonBlank(address, "country");
        if (country.isBlank()) {
            country = "India";
        }

        String state = firstNonBlank(address, "state");
        String district = firstNonBlank(address, "state_district", "district");
        String town = firstNonBlank(address, "city", "town", "municipality");
        String village = firstNonBlank(address, "village", "hamlet");
        String county = stripMandalSuffix(firstNonBlank(address, "county"));
        String suburb = firstNonBlank(address, "suburb", "neighbourhood", "quarter", "residential", "locality");

        String city;
        String area;
        if (!village.isBlank()) {
            area = village;
            // OSM county = mandal in AP/Telangana; prefer mandal over district for pincode fallback.
            String mandal = !county.isBlank() ? county : stripMandalSuffix(town);
            city = !mandal.isBlank() ? mandal : (!district.isBlank() ? district : village);
        } else if (!suburb.isBlank()) {
            area = suburb;
            city = !town.isBlank() ? town : county;
        } else {
            city = !town.isBlank() ? town : county;
            area = suburb;
        }

        String pincode = firstNonBlank(address, "postcode");
        if (!PINCODE.matcher(pincode).matches() && !area.isBlank()) {
            String resolved = indiaPincodeLookupService.resolvePincode(area, city, district, state);
            if (!resolved.isBlank()) {
                pincode = resolved;
            }
        }

        if (city.isBlank()) {
            city = extractFromDisplayName(node.path("display_name").asText(""), 0);
        }
        if (state.isBlank()) {
            state = extractFromDisplayName(node.path("display_name").asText(""), 2);
        }
        if (area.isBlank()) {
            area = city;
        }
        if (city.isBlank() && area.isBlank()) {
            return null;
        }
        if (city.isBlank()) {
            city = area;
        }

        String externalId = node.path("place_id").asText("");
        String displayName = node.path("display_name").asText("");

        return LocationSearchResponse.builder()
                .externalId(externalId.isBlank() ? null : externalId)
                .displayName(displayName.isBlank() ? null : displayName)
                .external(true)
                .pincodeId(0)
                .pincode(pincode)
                .areaId(0)
                .area(area)
                .cityId(0)
                .city(city)
                .stateId(0)
                .state(state)
                .countryId(0)
                .country(country)
                .build();
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

    private static String extractFromDisplayName(String displayName, int index) {
        if (displayName == null || displayName.isBlank()) {
            return "";
        }
        String[] parts = displayName.split(",");
        if (index >= parts.length) {
            return "";
        }
        return parts[index].trim();
    }

    private static String stripMandalSuffix(String value) {
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
}
