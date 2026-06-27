package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.LocationSearchResponse;
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
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Google Maps-style place autocomplete and place details for India.
 * Autocomplete: Photon (OSM). Place details on select: Nominatim reverse geocode.
 */
@Service
@RequiredArgsConstructor
public class LocationPlacesService {

    private static final String PHOTON_URL = "https://photon.komoot.io/api/";
    private static final String NOMINATIM_REVERSE_URL = "https://nominatim.openstreetmap.org/reverse";
    /** India bounding box: minLon, minLat, maxLon, maxLat */
    private static final String INDIA_BBOX = "68.1,6.7,97.4,35.5";
    private static final String USER_AGENT = "SellerApp/1.0 (seller-address-onboarding)";
    private static final Pattern PINCODE = Pattern.compile("^\\d{6}$");
    private static final Set<String> EXCLUDED_OSM_KEYS = Set.of(
            "amenity", "railway", "highway", "shop", "building", "tourism", "leisure"
    );
    private static final int AUTocomplete_LIMIT = 12;

    private final ObjectMapper objectMapper;
    private final IndiaPincodeLookupService indiaPincodeLookupService;
    private final NominatimAdminLookupService nominatimAdminLookupService;
    private final RestClient restClient = RestClient.create();

    /** Maps-style suggestions — real geographic places only, ranked by Photon. */
    public List<LocationSearchResponse> autocomplete(String query, String countryFilter) {
        if (query == null || query.isBlank() || query.trim().length() < 2) {
            return List.of();
        }
        if (query.chars().allMatch(Character::isDigit)) {
            return List.of();
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(PHOTON_URL)
                    .queryParam("q", query.trim())
                    .queryParam("limit", AUTocomplete_LIMIT)
                    .queryParam("lang", "en");

            if (useIndiaBbox(countryFilter)) {
                builder.queryParam("bbox", INDIA_BBOX);
            }

            String body = restClient.get()
                    .uri(builder.build().encode().toUri())
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode features = root.path("features");
            if (!features.isArray()) {
                return List.of();
            }

            LinkedHashMap<String, LocationSearchResponse> deduped = new LinkedHashMap<>();
            for (JsonNode feature : features) {
                LocationSearchResponse item = mapPhotonFeature(feature, countryFilter);
                if (item == null) {
                    continue;
                }
                String key = normalize(item.getArea()) + "|" + normalize(item.getState());
                deduped.putIfAbsent(key, item);
            }
            return new ArrayList<>(deduped.values());
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Place Details — reverse geocode at coordinates when user taps a suggestion. */
    public LocationSearchResponse resolvePlace(double latitude, double longitude) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(NOMINATIM_REVERSE_URL)
                    .queryParam("lat", latitude)
                    .queryParam("lon", longitude)
                    .queryParam("format", "json")
                    .queryParam("addressdetails", "1")
                    .queryParam("zoom", "18")
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
                return null;
            }

            JsonNode node = objectMapper.readTree(body);
            LocationSearchResponse item = mapNominatimReverse(node);
            if (item == null) {
                return null;
            }

            if (!PINCODE.matcher(safe(item.getPincode())).matches()) {
                String mandal = item.getMandal() != null ? item.getMandal() : item.getCity();
                String district = item.getDistrict() != null ? item.getDistrict() : "";
                String pincode = indiaPincodeLookupService.resolvePincode(
                        item.getArea(), mandal, district, item.getState());
                if (PINCODE.matcher(pincode).matches()) {
                    item = item.toBuilder().pincode(pincode).build();
                }
            }
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    private LocationSearchResponse mapPhotonFeature(JsonNode feature, String countryFilter) {
        JsonNode props = feature.path("properties");
        if (props.isMissingNode()) {
            return null;
        }

        String osmKey = props.path("osm_key").asText("");
        if (EXCLUDED_OSM_KEYS.contains(osmKey)) {
            return null;
        }
        if ("boundary".equals(osmKey) && "administrative".equals(props.path("osm_value").asText(""))) {
            return null;
        }

        String country = props.path("country").asText("").trim();
        String countryCode = props.path("countrycode").asText("").trim();
        if (!matchesCountry(country, countryCode, countryFilter)) {
            return null;
        }

        String name = props.path("name").asText("").trim();
        if (name.isBlank()) {
            return null;
        }

        String county = stripAdminSuffix(props.path("county").asText("").trim());
        String state = props.path("state").asText("").trim();
        String pincode = props.path("postcode").asText("").trim();

        JsonNode coords = feature.path("geometry").path("coordinates");
        Double lon = coords.isArray() && coords.size() >= 2 ? coords.get(0).asDouble() : null;
        Double lat = coords.isArray() && coords.size() >= 2 ? coords.get(1).asDouble() : null;

        String mandal = county;
        if (mandal.isBlank()) {
            NominatimAdminLookupService.AdminDivisions admin = nominatimAdminLookupService.lookup(name, state);
            if (admin.mandal() != null && !admin.mandal().isBlank()) {
                mandal = NominatimAdminLookupService.normalizeMandalName(admin.mandal());
            }
        }

        String district = resolveDistrict(name, state, lat, lon);
        String city = resolveCityForHierarchy(name, mandal.isBlank() ? name : mandal, district);

        if (!PINCODE.matcher(pincode).matches() && lat != null && lon != null) {
            String mandalForPin = mandal.isBlank() ? name : mandal;
            String resolved = indiaPincodeLookupService.resolvePincode(name, mandalForPin, district, state);
            if (PINCODE.matcher(resolved).matches()) {
                pincode = resolved;
            }
        }

        String osmType = props.path("osm_type").asText("");
        String osmId = props.path("osm_id").asText("");
        String externalId = osmType.isBlank() || osmId.isBlank() ? null : osmType + ":" + osmId;

        String displayName = buildDisplayName(name, mandal, district, state, pincode, country.isBlank() ? "India" : country);

        return LocationSearchResponse.builder()
                .externalId(externalId)
                .displayName(displayName)
                .district(district.isBlank() ? null : district)
                .mandal(mandal.isBlank() ? name : mandal)
                .external(true)
                .pincodeId(0)
                .pincode(PINCODE.matcher(pincode).matches() ? pincode : "")
                .areaId(0)
                .area(name)
                .cityId(0)
                .city(city)
                .stateId(0)
                .state(state)
                .countryId(0)
                .country(country.isBlank() ? "India" : country)
                .latitude(lat)
                .longitude(lon)
                .build();
    }

    private LocationSearchResponse mapNominatimReverse(JsonNode node) {
        JsonNode address = node.path("address");
        if (address.isMissingNode() || address.isNull()) {
            return null;
        }

        double lat = node.path("lat").asDouble(0);
        double lon = node.path("lon").asDouble(0);
        String externalId = node.path("osm_type").asText("") + ":" + node.path("osm_id").asText("");

        String country = firstNonBlank(address, "country");
        if (country.isBlank()) {
            country = "India";
        }

        String state = firstNonBlank(address, "state");
        String district = firstNonBlank(address, "state_district", "district");
        String county = stripAdminSuffix(firstNonBlank(address, "county"));
        String town = firstNonBlank(address, "city", "town", "municipality");
        String village = firstNonBlank(address, "village", "hamlet");
        String suburb = firstNonBlank(address, "suburb", "neighbourhood", "locality", "quarter");

        String area;
        String mandal;
        if (!village.isBlank()) {
            area = village;
            mandal = !county.isBlank() && !county.equalsIgnoreCase(village) ? county : stripAdminSuffix(town);
        } else if (!town.isBlank()) {
            area = town;
            mandal = !county.isBlank() ? county : town;
        } else if (!suburb.isBlank()) {
            area = suburb;
            mandal = !town.isBlank() ? town : county;
        } else {
            area = firstNonBlank(address, "road", "pedestrian");
            if (area.isBlank()) {
                area = !town.isBlank() ? town : suburb;
            }
            mandal = !county.isBlank() ? county : town;
        }

        if (district.isBlank() && lat != 0 && lon != 0) {
            district = lookupDistrictAtCoordinates(lat, lon);
        }
        if (district.isBlank() && !area.isBlank()) {
            district = resolveDistrict(area, state, lat != 0 ? lat : null, lon != 0 ? lon : null);
        }

        if (mandal.isBlank()) {
            mandal = area;
        }
        String city = resolveCityForHierarchy(area, mandal, district);

        if (area.isBlank()) {
            area = mandal;
        }

        String pincode = firstNonBlank(address, "postcode");
        if (!PINCODE.matcher(pincode).matches()) {
            String resolved = indiaPincodeLookupService.resolvePincode(area, mandal, district, state);
            if (PINCODE.matcher(resolved).matches()) {
                pincode = resolved;
            }
        }

        String displayName = buildDisplayName(area, mandal, district, state, pincode, country);

        return LocationSearchResponse.builder()
                .externalId(externalId.isBlank() ? null : externalId)
                .displayName(displayName)
                .district(district.isBlank() ? null : district)
                .mandal(mandal.isBlank() ? null : mandal)
                .external(true)
                .pincodeId(0)
                .pincode(PINCODE.matcher(pincode).matches() ? pincode : "")
                .areaId(0)
                .area(area)
                .cityId(0)
                .city(city)
                .stateId(0)
                .state(state)
                .countryId(0)
                .country(country)
                .latitude(lat != 0 ? lat : null)
                .longitude(lon != 0 ? lon : null)
                .build();
    }

    private static String buildDisplayName(
            String village, String mandal, String district, String state, String pincode, String country) {
        List<String> parts = new ArrayList<>();
        if (!village.isBlank()) {
            parts.add(village);
        }
        if (!mandal.isBlank() && !mandal.equalsIgnoreCase(village)) {
            parts.add(mandal);
        }
        if (district != null && !district.isBlank()
                && !district.equalsIgnoreCase(mandal)
                && !district.equalsIgnoreCase(village)) {
            parts.add(district);
        }
        if (!state.isBlank()) {
            parts.add(state);
        }
        if (PINCODE.matcher(safe(pincode)).matches()) {
            parts.add(pincode);
        }
        return String.join(", ", parts);
    }

    /**
     * City field = district (e.g. Palnadu). Mandal stays in suggestions only.
     */
    private static String resolveCityForHierarchy(String area, String mandal, String district) {
        if (district != null && !district.isBlank()) {
            return district.trim();
        }
        String mandalNorm = mandal != null ? mandal.trim() : "";
        String areaNorm = area != null ? area.trim() : "";
        if (!mandalNorm.isBlank() && !mandalNorm.equalsIgnoreCase(areaNorm)) {
            return mandalNorm;
        }
        return areaNorm;
    }

    /** District (state_district) — e.g. Palnadu. Coordinates lookup first, then Nominatim by name. */
    private String resolveDistrict(String placeName, String state, Double lat, Double lon) {
        if (lat != null && lon != null) {
            String fromCoords = lookupDistrictAtCoordinates(lat, lon);
            if (!fromCoords.isBlank()) {
                return fromCoords;
            }
        }
        if (placeName != null && !placeName.isBlank()) {
            NominatimAdminLookupService.AdminDivisions admin = nominatimAdminLookupService.lookup(placeName, state);
            if (admin.district() != null && !admin.district().isBlank()) {
                return admin.district().trim();
            }
        }
        return "";
    }

    private static boolean useIndiaBbox(String countryFilter) {
        if (countryFilter == null || countryFilter.isBlank()) {
            return true;
        }
        String norm = countryFilter.trim().toLowerCase(Locale.ROOT);
        return norm.equals("india") || norm.equals("in") || norm.equals("bharat");
    }

    private static boolean matchesCountry(String country, String countryCode, String countryFilter) {
        if (countryFilter == null || countryFilter.isBlank()) {
            return true;
        }
        String filter = countryFilter.trim().toLowerCase(Locale.ROOT);
        if (filter.equals("india") || filter.equals("in") || filter.equals("bharat")) {
            return "in".equalsIgnoreCase(countryCode) || country.toLowerCase(Locale.ROOT).contains("india");
        }
        return country.toLowerCase(Locale.ROOT).contains(filter)
                || filter.contains(country.toLowerCase(Locale.ROOT));
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

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /** District (state_district) from admin-level reverse geocode — e.g. Palnadu for Macherla. */
    private String lookupDistrictAtCoordinates(double latitude, double longitude) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(NOMINATIM_REVERSE_URL)
                    .queryParam("lat", latitude)
                    .queryParam("lon", longitude)
                    .queryParam("format", "json")
                    .queryParam("addressdetails", "1")
                    .queryParam("zoom", "10")
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

            JsonNode node = objectMapper.readTree(body);
            return firstNonBlank(node.path("address"), "state_district", "district");
        } catch (Exception e) {
            return "";
        }
    }
}
