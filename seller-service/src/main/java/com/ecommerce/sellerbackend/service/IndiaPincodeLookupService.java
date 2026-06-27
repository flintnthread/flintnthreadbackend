package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.LocationSearchResponse;
import com.ecommerce.sellerbackend.util.LocationFuzzyMatcher;
import com.ecommerce.sellerbackend.util.LocationSearchNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * India Post pincode data — search villages/post offices and resolve missing pincodes.
 * Free API at postalpincode.in (no API key).
 */
@Service
@RequiredArgsConstructor
public class IndiaPincodeLookupService {

    private static final String POST_OFFICE_URL = "https://api.postalpincode.in/postoffice/";
    private static final String PINCODE_URL = "https://api.postalpincode.in/pincode/";
    private static final Pattern PINCODE = Pattern.compile("^\\d{6}$");

    private static final String USER_AGENT = "SellerApp/1.0 (location pincode lookup)";

    private final ObjectMapper objectMapper;
    private final NominatimAdminLookupService nominatimAdminLookupService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Map<String, JsonNode> pincodeOfficeCache = new LinkedHashMap<>();

    /**
     * Search post offices by village / locality name — fast, includes pincode.
     */
    public List<LocationSearchResponse> searchPostOffices(String query, String stateHint) {
        if (query == null || query.isBlank() || query.trim().length() < 2) {
            return List.of();
        }
        if (query.chars().allMatch(Character::isDigit)) {
            return List.of();
        }

        String trimmed = query.trim();
        LinkedHashMap<String, LocationSearchResponse> results = new LinkedHashMap<>();
        LinkedHashSet<String> tried = new LinkedHashSet<>();
        for (String term : searchTerms(trimmed)) {
            if (tried.add(term.toLowerCase(Locale.ROOT))) {
                mergeOffices(results, fetchOffices(term), stateHint);
            }
        }
        if (needsFuzzyExpansion(trimmed, results)) {
            for (String term : fuzzySearchTerms(trimmed)) {
                if (!tried.add(term.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                mergeOffices(results, fetchOffices(term), stateHint);
                if (results.size() >= 80) {
                    break;
                }
            }
        }
        return rankPostOffices(new ArrayList<>(results.values()), trimmed);
    }

    private boolean needsFuzzyExpansion(String query, LinkedHashMap<String, LocationSearchResponse> results) {
        if (results.isEmpty()) {
            return true;
        }
        if (LocationFuzzyMatcher.normalize(query).length() < 5) {
            return false;
        }
        double best = 0;
        for (LocationSearchResponse item : results.values()) {
            best = Math.max(best, scorePostOffice(item, query));
        }
        return best < 0.75;
    }

    /**
     * Resolve pincode for a place name when geocoder did not return one.
     */
    public String lookupByPlaceName(String placeName, String stateHint) {
        if (placeName == null || placeName.isBlank()) {
            return "";
        }
        for (String term : searchTerms(placeName.trim())) {
            String pincode = firstPincode(fetchOffices(term), stateHint);
            if (isValidPincode(pincode)) {
                return pincode;
            }
        }
        return "";
    }

    /**
     * Village → mandal (Block) → district fallback — same as Google Maps for rural India.
     * Example: Cherlagudipadu village → Gurazala mandal → 522415
     */
    public String resolvePincode(String area, String mandal, String district, String stateHint) {
        if (area != null && !area.isBlank()) {
            String pin = lookupWithMandalBlockFallback(area.trim(), stateHint);
            if (isValidPincode(pin)) {
                return pin;
            }
        }

        if (mandal != null && !mandal.isBlank()) {
            for (String term : searchTerms(stripAdminSuffix(mandal))) {
                String pin = firstPincode(fetchOffices(term), stateHint);
                if (isValidPincode(pin)) {
                    return pin;
                }
            }
            String pinFromBlock = lookupPincodeByMandalBlock(mandal, stateHint);
            if (isValidPincode(pinFromBlock)) {
                return pinFromBlock;
            }
        }

        if (district != null && !district.isBlank()
                && (mandal == null || !stripAdminSuffix(district).equalsIgnoreCase(stripAdminSuffix(mandal)))) {
            String pin = lookupByPlaceName(district.trim(), stateHint);
            if (isValidPincode(pin)) {
                return pin;
            }
        }

        return "";
    }

    /**
     * Find mandal pincode by matching Block field across offices for a village in that mandal.
     */
    private String lookupPincodeByMandalBlock(String mandal, String stateHint) {
        if (mandal == null || mandal.isBlank()) {
            return "";
        }
        String mandalNorm = normalize(stripAdminSuffix(mandal));
        for (String term : searchTerms(stripAdminSuffix(mandal))) {
            JsonNode offices = fetchOffices(term);
            if (offices == null || !offices.isArray()) {
                continue;
            }
            String stateNorm = normalize(stateHint);
            for (JsonNode office : offices) {
                if (!stateNorm.isEmpty()) {
                    String officeState = normalize(office.path("State").asText(""));
                    if (!officeState.isEmpty() && !officeState.equals(stateNorm)) {
                        continue;
                    }
                }
                String block = normalize(stripAdminSuffix(office.path("Block").asText("")));
                if (!block.isEmpty() && (block.equals(mandalNorm) || block.startsWith(mandalNorm) || mandalNorm.startsWith(block))) {
                    String pin = office.path("Pincode").asText("").trim();
                    if (isValidPincode(pin)) {
                        return pin;
                    }
                }
            }
        }
        return "";
    }

    private String lookupWithMandalBlockFallback(String placeName, String stateHint) {
        for (String term : searchTerms(placeName)) {
            JsonNode offices = fetchOffices(term);
            if (offices == null || !offices.isArray()) {
                continue;
            }

            String exact = firstPincodeMatchingName(offices, placeName, stateHint);
            if (isValidPincode(exact)) {
                return exact;
            }

            String any = firstPincode(offices, stateHint);
            if (isValidPincode(any)) {
                return any;
            }

            for (String block : collectBlockNames(offices, stateHint)) {
                String blockPin = firstPincode(fetchOffices(block), stateHint);
                if (isValidPincode(blockPin)) {
                    return blockPin;
                }
            }
        }
        return "";
    }

    private String firstPincodeMatchingName(JsonNode offices, String placeName, String stateHint) {
        if (offices == null || !offices.isArray() || placeName == null || placeName.isBlank()) {
            return "";
        }
        String placeNorm = normalize(placeName);
        String stateNorm = normalize(stateHint);
        for (JsonNode office : offices) {
            String name = normalize(office.path("Name").asText(""));
            if (name.isBlank() || !(name.equals(placeNorm) || name.startsWith(placeNorm + " "))) {
                continue;
            }
            String pincode = office.path("Pincode").asText("").trim();
            if (!isValidPincode(pincode)) {
                continue;
            }
            if (!stateNorm.isEmpty()) {
                String officeState = normalize(office.path("State").asText(""));
                if (!officeState.isEmpty() && !officeState.equals(stateNorm)) {
                    continue;
                }
            }
            return pincode;
        }
        return "";
    }

    private List<String> collectBlockNames(JsonNode offices, String stateHint) {
        LinkedHashMap<String, String> blocks = new LinkedHashMap<>();
        if (offices == null || !offices.isArray()) {
            return List.of();
        }
        String stateNorm = normalize(stateHint);
        for (JsonNode office : offices) {
            if (!stateNorm.isEmpty()) {
                String officeState = normalize(office.path("State").asText(""));
                if (!officeState.isEmpty() && !officeState.equals(stateNorm)) {
                    continue;
                }
            }
            String block = stripAdminSuffix(office.path("Block").asText("").trim());
            if (!block.isBlank()) {
                blocks.putIfAbsent(block.toLowerCase(Locale.ROOT), block);
            }
        }
        return new ArrayList<>(blocks.values());
    }

    private static String stripAdminSuffix(String value) {
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

    private static boolean isValidPincode(String pincode) {
        return pincode != null && PINCODE.matcher(pincode.trim()).matches();
    }

    /**
     * Look up all post offices for a 6-digit pincode (India Post API).
     */
    public List<LocationSearchResponse> lookupByPincode(String pincode) {
        String code = pincode != null ? pincode.trim() : "";
        if (!PINCODE.matcher(code).matches()) {
            return List.of();
        }

        LinkedHashMap<String, LocationSearchResponse> results = new LinkedHashMap<>();
        mergeOffices(results, fetchByPincode(code), null);
        return new ArrayList<>(results.values());
    }

    private List<String> searchTerms(String query) {
        LinkedHashMap<String, String> terms = new LinkedHashMap<>();
        terms.put(query.toLowerCase(Locale.ROOT), query);
        String title = toTitleCase(query);
        if (!title.equalsIgnoreCase(query)) {
            terms.put(title.toLowerCase(Locale.ROOT), title);
        }
        addSpellingVariants(terms, query);
        return new ArrayList<>(terms.values());
    }

    /** Typo variants and short prefixes — used when exact search misses or scores poorly. */
    private List<String> fuzzySearchTerms(String query) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        for (String expanded : LocationSearchNormalizer.expandTerms(query)) {
            terms.add(expanded);
            String titled = toTitleCase(expanded);
            if (!titled.equalsIgnoreCase(expanded)) {
                terms.add(titled);
            }
        }
        for (String prefix : LocationFuzzyMatcher.prefixTerms(query)) {
            terms.add(prefix);
            terms.add(toTitleCase(prefix));
        }
        List<String> ordered = new ArrayList<>(terms);
        return ordered.subList(0, Math.min(10, ordered.size()));
    }

    private List<LocationSearchResponse> rankPostOffices(List<LocationSearchResponse> items, String query) {
        if (items.isEmpty()) {
            return items;
        }
        items.sort((a, b) -> Double.compare(scorePostOffice(b, query), scorePostOffice(a, query)));
        if (LocationFuzzyMatcher.normalize(query).length() < 5) {
            return items.subList(0, Math.min(30, items.size()));
        }
        List<LocationSearchResponse> filtered = new ArrayList<>();
        for (LocationSearchResponse item : items) {
            if (LocationFuzzyMatcher.passesRelevanceFilter(
                    query, item.getArea(), item.getCity(), item.getDisplayName())) {
                filtered.add(item);
            }
            if (filtered.size() >= 25) {
                break;
            }
        }
        return filtered.isEmpty() ? items.subList(0, Math.min(15, items.size())) : filtered;
    }

    private double scorePostOffice(LocationSearchResponse item, String query) {
        return LocationFuzzyMatcher.bestMatchScore(
                query, item.getArea(), item.getCity(), item.getState(), item.getDisplayName());
    }

    /** AP/Telangana mandal names often differ by one letter in India Post (Gurazala → Gurazalla). */
    private void addSpellingVariants(LinkedHashMap<String, String> terms, String query) {
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.endsWith("zala") && !lower.endsWith("zalla")) {
            String alt = trimmed.substring(0, trimmed.length() - 1) + "lla";
            terms.putIfAbsent(alt.toLowerCase(Locale.ROOT), alt);
        }
        if (lower.endsWith("ala") && !lower.endsWith("alla")) {
            String alt = trimmed + "la";
            terms.putIfAbsent(alt.toLowerCase(Locale.ROOT), alt);
        }
    }

    private static String toTitleCase(String value) {
        if (value.isBlank()) {
            return value;
        }
        String[] parts = value.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            String part = parts[i];
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1).toLowerCase(Locale.ROOT));
                }
            }
        }
        return sb.toString();
    }

    private JsonNode fetchOffices(String placeName) {
        String encoded = UriUtils.encodePath(placeName.trim(), StandardCharsets.UTF_8);
        return fetchPostOfficeNode(POST_OFFICE_URL + encoded);
    }

    private JsonNode fetchByPincode(String pincode) {
        return fetchPostOfficeNode(PINCODE_URL + pincode.trim());
    }

    private JsonNode fetchPostOfficeNode(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }

            String body = response.body();
            if (body == null || body.isBlank()) {
                return null;
            }

            JsonNode root = objectMapper.readTree(body);
            if (!root.isArray() || root.isEmpty()) {
                return null;
            }

            JsonNode first = root.get(0);
            if (!"Success".equalsIgnoreCase(first.path("Status").asText(""))) {
                return null;
            }

            JsonNode postOffice = first.get("PostOffice");
            if (postOffice == null || postOffice.isNull()) {
                return null;
            }
            if (postOffice.isArray()) {
                return postOffice;
            }
            return objectMapper.createArrayNode().add(postOffice);
        } catch (Exception e) {
            return null;
        }
    }

    private void mergeOffices(
            LinkedHashMap<String, LocationSearchResponse> target,
            JsonNode offices,
            String stateHint) {
        if (offices == null || !offices.isArray()) {
            return;
        }

        String stateNorm = normalize(stateHint);
        for (JsonNode office : offices) {
            LocationSearchResponse item = mapOffice(office, stateNorm);
            if (item == null) {
                continue;
            }
            String key = normalize(item.getArea()) + "|" + normalize(item.getPincode()) + "|" + normalize(item.getState());
            target.putIfAbsent(key, item);
        }
    }

    private String firstPincode(JsonNode offices, String stateHint) {
        if (offices == null || !offices.isArray()) {
            return "";
        }
        String stateNorm = normalize(stateHint);
        for (JsonNode office : offices) {
            String pincode = office.path("Pincode").asText("").trim();
            if (!isValidPincode(pincode)) {
                continue;
            }
            if (!stateNorm.isEmpty()) {
                String officeState = normalize(office.path("State").asText(""));
                if (!officeState.isEmpty() && !officeState.equals(stateNorm)) {
                    continue;
                }
            }
            return pincode;
        }
        for (JsonNode office : offices) {
            String pincode = office.path("Pincode").asText("").trim();
            if (PINCODE.matcher(pincode).matches()) {
                return pincode;
            }
        }
        return "";
    }

    private LocationSearchResponse mapOffice(JsonNode office, String stateNorm) {
        String name = office.path("Name").asText("").trim();
        String pincode = office.path("Pincode").asText("").trim();
        String postalDistrict = office.path("District").asText("").trim();
        String block = NominatimAdminLookupService.stripAdminSuffix(office.path("Block").asText("").trim());
        String state = office.path("State").asText("").trim();

        if (name.isBlank() || !isValidPincode(pincode)) {
            return null;
        }
        if (!stateNorm.isEmpty() && !state.isBlank() && !normalize(state).equals(stateNorm)) {
            return null;
        }

        if (block.isBlank()) {
            block = lookupBlockFromPincode(pincode, name);
        }
        String mandal = NominatimAdminLookupService.normalizeMandalName(block);

        NominatimAdminLookupService.AdminDivisions osm = nominatimAdminLookupService.lookup(name, state);
        if (mandal.isBlank() && osm.mandal() != null && !osm.mandal().isBlank()) {
            mandal = NominatimAdminLookupService.normalizeMandalName(osm.mandal());
        }

        String district = osm.district() != null ? osm.district().trim() : "";
        if (district.isBlank()) {
            district = postalDistrict;
        }

        // City in our hierarchy = mandal (Block), not stale India Post district.
        String city = !mandal.isBlank() ? mandal : (!district.isBlank() ? district : name);

        List<String> displayParts = new ArrayList<>();
        displayParts.add(name);
        if (!mandal.isBlank() && !mandal.equalsIgnoreCase(name)) {
            displayParts.add(mandal);
        }
        if (!district.isBlank()
                && !district.equalsIgnoreCase(mandal)
                && !district.equalsIgnoreCase(name)) {
            displayParts.add(district);
        }
        displayParts.add(state);
        displayParts.add(pincode);
        displayParts.add("India");
        String displayName = String.join(", ", displayParts);

        return LocationSearchResponse.builder()
                .externalId("post-" + pincode + "-" + name.replaceAll("\\s+", "-").toLowerCase(Locale.ROOT))
                .displayName(displayName)
                .external(true)
                .pincodeId(0)
                .pincode(pincode)
                .areaId(0)
                .area(name)
                .cityId(0)
                .city(city)
                .stateId(0)
                .state(state)
                .countryId(0)
                .country("India")
                .build();
    }

    /** India Post postoffice search omits Block; pincode API includes it (mandal). */
    private String lookupBlockFromPincode(String pincode, String officeName) {
        if (!isValidPincode(pincode) || officeName == null || officeName.isBlank()) {
            return "";
        }
        JsonNode offices = pincodeOfficeCache.get(pincode);
        if (offices == null) {
            offices = fetchByPincode(pincode);
            if (offices != null && pincodeOfficeCache.size() < 100) {
                pincodeOfficeCache.put(pincode, offices);
            }
        }
        if (offices == null || !offices.isArray()) {
            return "";
        }

        String nameNorm = normalize(officeName);
        for (JsonNode entry : offices) {
            String entryName = normalize(entry.path("Name").asText(""));
            if (entryName.equals(nameNorm) || entryName.startsWith(nameNorm) || nameNorm.startsWith(entryName)) {
                String block = NominatimAdminLookupService.stripAdminSuffix(entry.path("Block").asText("").trim());
                if (!block.isBlank()) {
                    return block;
                }
            }
        }
        for (JsonNode entry : offices) {
            String block = NominatimAdminLookupService.stripAdminSuffix(entry.path("Block").asText("").trim());
            if (!block.isBlank()) {
                return block;
            }
        }
        return "";
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
