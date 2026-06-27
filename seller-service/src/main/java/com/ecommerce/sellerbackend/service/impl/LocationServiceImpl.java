package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.AddPincodeRequest;
import com.ecommerce.sellerbackend.dto.CreateLocationRequest;
import com.ecommerce.sellerbackend.dto.LocationItemResponse;
import com.ecommerce.sellerbackend.dto.LocationSearchResponse;
import com.ecommerce.sellerbackend.repository.LocationRepository;
import com.ecommerce.sellerbackend.service.IndiaPincodeLookupService;
import com.ecommerce.sellerbackend.service.LocationPlacesService;
import com.ecommerce.sellerbackend.service.LocationService;
import com.ecommerce.sellerbackend.util.LocationSearchNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private static final int SEARCH_RESULT_LIMIT = 15;

    private final LocationRepository locationRepository;
    private final LocationPlacesService locationPlacesService;
    private final IndiaPincodeLookupService indiaPincodeLookupService;

    @Override
    @Transactional(readOnly = true)
    public List<LocationItemResponse> listCountries(String search) {
        return searchItems(
                search,
                term -> mapSimple(locationRepository.searchCountries(term)),
                LocationItemResponse::getId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationItemResponse> listStates(Integer countryId, String search) {
        return searchItems(
                search,
                term -> mapWithParent(locationRepository.searchStates(countryId, term), 2, 3),
                LocationItemResponse::getId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationItemResponse> listCities(Integer stateId, String search) {
        return searchItems(
                search,
                term -> mapWithParent(locationRepository.searchCities(stateId, term), 2, 3),
                LocationItemResponse::getId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationItemResponse> listAreas(Integer cityId, String search) {
        return searchItems(
                search,
                term -> mapWithParent(locationRepository.searchAreas(cityId, term), 2, 3),
                LocationItemResponse::getId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationItemResponse> listPincodes(Integer areaId, String search) {
        return searchItems(
                search,
                term -> {
                    List<Object[]> rows = locationRepository.searchPincodes(areaId, term);
                    List<LocationItemResponse> items = new ArrayList<>(rows.size());
                    for (Object[] row : rows) {
                        items.add(LocationItemResponse.builder()
                                .id(((Number) row[0]).intValue())
                                .name(row[1] != null ? row[1].toString() : "")
                                .parentId(row[2] != null ? ((Number) row[2]).intValue() : null)
                                .parentName(row[3] != null ? row[3].toString() : null)
                                .build());
                    }
                    return items;
                },
                LocationItemResponse::getId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationSearchResponse> searchLocations(String search, String country) {
        try {
            return doSearchLocations(search, country);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<LocationSearchResponse> doSearchLocations(String search, String country) {
        String q = blankToNull(search);
        if (q == null) {
            return List.of();
        }
        String countryFilter = blankToNull(country);

        if (q.chars().allMatch(Character::isDigit)) {
            return searchByPincode(q, countryFilter);
        }

        // Google Maps-style: Photon first (has mandal + district), DB only as fallback.
        LinkedHashMap<String, LocationSearchResponse> merged = new LinkedHashMap<>();

        for (LocationSearchResponse place : locationPlacesService.autocomplete(q, countryFilter)) {
            String key = normalizeKey(place.getArea()) + "|" + normalizeKey(place.getState());
            merged.put(key, place);
            if (merged.size() >= SEARCH_RESULT_LIMIT) {
                break;
            }
        }

        for (Object[] row : locationRepository.searchLocations(q, countryFilter)) {
            LocationSearchResponse item = withAreaPincode(mapSearchRow(row));
            String key = normalizeKey(item.getArea()) + "|" + normalizeKey(item.getState());
            merged.putIfAbsent(key, item);
            if (merged.size() >= SEARCH_RESULT_LIMIT) {
                break;
            }
        }

        return new ArrayList<>(merged.values()).subList(0, Math.min(SEARCH_RESULT_LIMIT, Math.max(0, merged.size())));
    }

    private List<LocationSearchResponse> searchByPincode(String q, String countryFilter) {
        LinkedHashMap<String, LocationSearchResponse> merged = new LinkedHashMap<>();

        for (Object[] row : locationRepository.searchLocationsByPincode(q, countryFilter)) {
            LocationSearchResponse item = withAreaPincode(mapSearchRow(row));
            String key = "db-pin-" + (item.getPincodeId() != null && item.getPincodeId() > 0
                    ? item.getPincodeId()
                    : item.getAreaId());
            merged.putIfAbsent(key, item);
        }

        if (q.length() == 6) {
            for (LocationSearchResponse postal : indiaPincodeLookupService.lookupByPincode(q)) {
                if (matchesCountryFilter(postal, countryFilter)) {
                    String key = "post-pin-" + postal.getPincode() + "-" + normalizeKey(postal.getArea());
                    merged.putIfAbsent(key, postal);
                }
            }
        }

        return new ArrayList<>(merged.values());
    }

    @Override
    @Transactional(readOnly = true)
    public LocationSearchResponse resolvePlace(double latitude, double longitude) {
        LocationSearchResponse resolved = locationPlacesService.resolvePlace(latitude, longitude);
        if (resolved == null) {
            return LocationSearchResponse.builder().build();
        }
        return resolved;
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    @Transactional
    public LocationSearchResponse saveLocation(CreateLocationRequest request) {
        String country = normalizeName(request.getCountry());
        String state = normalizeName(request.getState());
        String city = normalizeName(request.getCity());
        String area = normalizeName(request.getArea());
        String pincode = request.getPincode() != null ? request.getPincode().trim() : "";

        Integer countryId = locationRepository.findCountryIdByName(country);
        if (countryId == null) {
            countryId = locationRepository.insertCountry(country);
        }

        Integer stateId = locationRepository.findStateIdByName(countryId, state);
        if (stateId == null) {
            stateId = locationRepository.insertState(countryId, state);
        }

        Integer cityId = locationRepository.findCityIdByName(stateId, city);
        if (cityId == null) {
            cityId = locationRepository.insertCity(stateId, countryId, city);
        }

        Integer areaId = locationRepository.findAreaIdByName(cityId, area);
        if (areaId == null) {
            areaId = locationRepository.insertArea(cityId, stateId, countryId, area);
        }

        Integer pincodeId = 0;
        if (!pincode.isBlank()) {
            pincodeId = locationRepository.findPincodeIdByAreaAndCode(areaId, pincode);
            if (pincodeId == null) {
                pincodeId = locationRepository.insertPincode(countryId, stateId, cityId, areaId, pincode);
            }
        }

        return LocationSearchResponse.builder()
                .external(false)
                .pincodeId(pincodeId != null ? pincodeId : 0)
                .pincode(pincode)
                .areaId(areaId)
                .area(area)
                .cityId(cityId)
                .city(city)
                .stateId(stateId)
                .state(state)
                .countryId(countryId)
                .country(country)
                .build();
    }

    @Override
    @Transactional
    public LocationItemResponse addPincode(AddPincodeRequest request) {
        Integer areaId = request.getAreaId();
        String pincode = request.getPincode().trim();

        Integer pincodeId = locationRepository.findPincodeIdByAreaAndCode(areaId, pincode);
        if (pincodeId != null) {
            return LocationItemResponse.builder()
                    .id(pincodeId)
                    .name(pincode)
                    .parentId(areaId)
                    .build();
        }

        Integer[] hierarchy = locationRepository.findAreaHierarchyIds(areaId);
        if (hierarchy == null) {
            throw new IllegalArgumentException("Area not found");
        }
        pincodeId = locationRepository.insertPincode(
                hierarchy[0], hierarchy[1], hierarchy[2], areaId, pincode);

        return LocationItemResponse.builder()
                .id(pincodeId)
                .name(pincode)
                .parentId(areaId)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LocationSearchResponse resolveLocation(String country, String state, String city, String area, String pincode) {
        String countryName = country != null ? country.trim() : "";
        String stateName = state != null ? state.trim() : "";
        String cityName = city != null ? city.trim() : "";
        String areaName = area != null ? area.trim() : "";
        String requestedPincode = pincode != null ? pincode.trim() : "";

        LocationSearchResponse.LocationSearchResponseBuilder builder = LocationSearchResponse.builder()
                .external(false)
                .pincodeId(0)
                .pincode("")
                .areaId(0)
                .area("")
                .cityId(0)
                .city("")
                .stateId(0)
                .state("")
                .countryId(0)
                .country("");

        if (countryName.isBlank()) {
            return builder.build();
        }

        builder.country(countryName);
        Integer countryId = locationRepository.findCountryIdByName(countryName);
        if (countryId != null) {
            builder.countryId(countryId);
        }

        if (!stateName.isBlank()) {
            builder.state(stateName);
        }
        if (!cityName.isBlank()) {
            builder.city(cityName);
        }
        if (!areaName.isBlank()) {
            builder.area(areaName);
        }

        if (countryId != null && !stateName.isBlank()) {
            Integer stateId = locationRepository.findStateIdByName(countryId, stateName);
            if (stateId != null) {
                builder.stateId(stateId);
                if (!cityName.isBlank()) {
                    Integer cityId = locationRepository.findCityIdByName(stateId, cityName);
                    if (cityId != null) {
                        builder.cityId(cityId);
                        if (!areaName.isBlank()) {
                            Integer areaId = locationRepository.findAreaIdByName(cityId, areaName);
                            if (areaId != null) {
                                builder.areaId(areaId);
                            }
                        }
                    }
                }
            }
        }

        String resolvedPincode = requestedPincode;
        if (resolvedPincode.isBlank()) {
            LocationSearchResponse partial = builder.build();
            if (partial.getAreaId() != null && partial.getAreaId() > 0) {
                String fromDb = locationRepository.findFirstPincodeByAreaId(partial.getAreaId());
                if (fromDb != null && !fromDb.isBlank()) {
                    resolvedPincode = fromDb.trim();
                }
            }
        }
        if (resolvedPincode.isBlank() && !areaName.isBlank()) {
            resolvedPincode = indiaPincodeLookupService.resolvePincode(areaName, cityName, null, stateName);
        }

        if (!resolvedPincode.isBlank()) {
            builder.pincode(resolvedPincode);
            LocationSearchResponse partial = builder.build();
            if (partial.getAreaId() != null && partial.getAreaId() > 0) {
                Integer pincodeId = locationRepository.findPincodeIdByAreaAndCode(partial.getAreaId(), resolvedPincode);
                builder.pincodeId(pincodeId != null ? pincodeId : 0);
            }
        }

        return builder.build();
    }

    @Override
    @Transactional(readOnly = true)
    public LocationSearchResponse enrichExternalLocation(LocationSearchResponse item) {
        if (item == null) {
            return null;
        }
        if (item.getLatitude() != null && item.getLongitude() != null) {
            LocationSearchResponse resolved = locationPlacesService.resolvePlace(
                    item.getLatitude(), item.getLongitude());
            if (resolved != null) {
                return resolved;
            }
        }
        if (item.getPincode() != null && !item.getPincode().isBlank()) {
            return item;
        }
        String pincode = indiaPincodeLookupService.resolvePincode(
                item.getArea(), item.getCity(), null, item.getState());
        if (pincode.isBlank()) {
            return item;
        }
        return item.toBuilder().pincode(pincode).build();
    }

    private LocationSearchResponse withAreaPincode(LocationSearchResponse item) {
        if (item.getPincode() != null && !item.getPincode().isBlank()) {
            return item;
        }
        if (item.getAreaId() == null || item.getAreaId() <= 0) {
            return item;
        }
        String pin = locationRepository.findFirstPincodeByAreaId(item.getAreaId());
        if (pin == null || pin.isBlank()) {
            return item;
        }
        Integer pincodeId = locationRepository.findPincodeIdByAreaAndCode(item.getAreaId(), pin);
        return item.toBuilder()
                .pincode(pin.trim())
                .pincodeId(pincodeId != null ? pincodeId : 0)
                .build();
    }

    private boolean matchesCountryFilter(LocationSearchResponse item, String countryFilter) {
        if (countryFilter == null || countryFilter.isBlank()) {
            return true;
        }
        if (item.getCountry() == null || item.getCountry().isBlank()) {
            return true;
        }
        return normalizeKey(item.getCountry()).equals(normalizeKey(countryFilter));
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Location name is required.");
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private <T> List<T> searchItems(
            String search,
            Function<String, List<T>> fetchForTerm,
            Function<T, Integer> idExtractor) {
        String q = blankToNull(search);
        if (q == null) {
            return fetchForTerm.apply(null);
        }

        LinkedHashMap<Integer, T> merged = new LinkedHashMap<>();
        for (String term : expandSearchTerms(q)) {
            for (T item : fetchForTerm.apply(term)) {
                merged.putIfAbsent(idExtractor.apply(item), item);
            }
        }
        return new ArrayList<>(merged.values());
    }

    private List<String> expandSearchTerms(String search) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        terms.add(search);
        terms.addAll(LocationSearchNormalizer.expandTerms(search));
        return new ArrayList<>(terms);
    }

    private LocationSearchResponse mapSearchRow(Object[] row) {
        Integer pincodeId = row[0] != null ? ((Number) row[0]).intValue() : null;
        String pincode = row[1] != null ? row[1].toString() : "";
        return LocationSearchResponse.builder()
                .external(false)
                .pincodeId(pincodeId != null ? pincodeId : 0)
                .pincode(pincode)
                .areaId(((Number) row[2]).intValue())
                .area(row[3] != null ? row[3].toString() : "")
                .cityId(((Number) row[4]).intValue())
                .city(row[5] != null ? row[5].toString() : "")
                .stateId(((Number) row[6]).intValue())
                .state(row[7] != null ? row[7].toString() : "")
                .countryId(((Number) row[8]).intValue())
                .country(row[9] != null ? row[9].toString() : "")
                .build();
    }

    private List<LocationItemResponse> mapSimple(List<Object[]> rows) {
        List<LocationItemResponse> items = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            items.add(LocationItemResponse.builder()
                    .id(((Number) row[0]).intValue())
                    .name(row[1] != null ? row[1].toString() : "")
                    .build());
        }
        return items;
    }

    private List<LocationItemResponse> mapWithParent(List<Object[]> rows, int parentIdIndex, int parentNameIndex) {
        List<LocationItemResponse> items = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            items.add(LocationItemResponse.builder()
                    .id(((Number) row[0]).intValue())
                    .name(row[1] != null ? row[1].toString() : "")
                    .parentId(row[parentIdIndex] != null ? ((Number) row[parentIdIndex]).intValue() : null)
                    .parentName(row[parentNameIndex] != null ? row[parentNameIndex].toString() : null)
                    .build());
        }
        return items;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
