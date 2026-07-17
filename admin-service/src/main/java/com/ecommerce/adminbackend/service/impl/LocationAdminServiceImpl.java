package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.repository.LocationRepository;
import com.ecommerce.adminbackend.service.LocationAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class LocationAdminServiceImpl extends BaseAdminService implements LocationAdminService {

    private final LocationRepository locationRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listCountries(String search, int page, int size) {
        String q = blankToNull(search);
        int safeSize = sanitizeSize(size);
        int safePage = Math.max(page, 0);
        List<Map<String, Object>> items = mapRows(locationRepository.searchCountries(q, safePage, safeSize), row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("name", row[1]);
            item.put("code", row[2]);
            item.put("active", toActive(row[3]));
            return item;
        });
        return toPage(items, locationRepository.countCountriesSearch(q), safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listStates(Integer countryId, String search, int page, int size) {
        String q = blankToNull(search);
        int safeSize = sanitizeSize(size);
        int safePage = Math.max(page, 0);
        List<Map<String, Object>> items = mapRows(locationRepository.searchStates(countryId, q, safePage, safeSize), row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("name", row[1]);
            item.put("countryId", row[2]);
            item.put("countryName", row[3]);
            item.put("active", toActive(row[4]));
            item.put("cityCount", toCount(row.length > 5 ? row[5] : 0));
            return item;
        });
        return toPage(items, locationRepository.countStatesSearch(countryId, q), safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listCities(Integer stateId, String search, int page, int size) {
        String q = blankToNull(search);
        int safeSize = sanitizeSize(size);
        int safePage = Math.max(page, 0);
        List<Map<String, Object>> items = mapRows(locationRepository.searchCities(stateId, q, safePage, safeSize), row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("name", row[1]);
            item.put("stateId", row[2]);
            item.put("stateName", row[3]);
            item.put("active", toActive(row[4]));
            item.put("areaCount", toCount(row.length > 5 ? row[5] : 0));
            return item;
        });
        return toPage(items, locationRepository.countCitiesSearch(stateId, q), safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listAreas(Integer cityId, String search, int page, int size) {
        String q = blankToNull(search);
        int safeSize = sanitizeSize(size);
        int safePage = Math.max(page, 0);
        List<Map<String, Object>> items = mapRows(locationRepository.searchAreas(cityId, q, safePage, safeSize), row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("name", row[1]);
            item.put("cityId", row[2]);
            item.put("cityName", row[3]);
            item.put("active", toActive(row[4]));
            item.put("pincodeCount", toCount(row.length > 5 ? row[5] : 0));
            return item;
        });
        return toPage(items, locationRepository.countAreasSearch(cityId, q), safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listPincodes(Integer cityId, Integer areaId, String search, int page, int size) {
        String q = blankToNull(search);
        int safeSize = sanitizeSize(size);
        int safePage = Math.max(page, 0);
        List<Map<String, Object>> items = mapRows(
                locationRepository.searchPincodes(cityId, areaId, q, safePage, safeSize),
                row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", row[0]);
                    item.put("pincode", row[1]);
                    item.put("area", row[2]);
                    item.put("city", row[3]);
                    item.put("state", row[4]);
                    item.put("country", row[5]);
                    item.put("cityId", row[6]);
                    item.put("areaId", row[7]);
                    item.put("name", row[1]);
                    return item;
                });
        return toPage(items, locationRepository.countPincodesSearch(cityId, areaId, q), safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("countries", locationRepository.countCountries());
        counts.put("states", locationRepository.countStates(null));
        counts.put("cities", locationRepository.countCities(null));
        counts.put("areas", locationRepository.countAreas(null));
        counts.put("pincodes", locationRepository.countPincodes());
        return counts;
    }

    @Override
    @Transactional
    public Map<String, Object> createCountry(String name, String code, Boolean active) {
        String countryName = requireNonBlank(name, "Country name");
        String countryCode = requireNonBlank(code, "Country code");
        if (countryCode.length() != 2) {
            throw new IllegalArgumentException("Country code must be exactly 2 characters.");
        }
        boolean isActive = active == null || active;
        Integer id = locationRepository.insertCountry(countryName, countryCode, isActive);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("name", countryName);
        result.put("code", countryCode.toUpperCase(Locale.ROOT));
        result.put("active", isActive);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createState(Integer countryId, String name, Boolean active) {
        if (countryId == null || countryId <= 0) {
            throw new IllegalArgumentException("Country is required.");
        }
        String stateName = requireNonBlank(name, "State name");
        boolean isActive = active == null || active;
        Integer id = locationRepository.insertState(countryId, stateName, isActive);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("name", stateName);
        result.put("countryId", countryId);
        result.put("active", isActive);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createCity(Integer stateId, String name, Boolean active) {
        if (stateId == null || stateId <= 0) {
            throw new IllegalArgumentException("State is required.");
        }
        Integer countryId = locationRepository.findCountryIdByStateId(stateId);
        if (countryId == null) {
            throw new IllegalArgumentException("Selected state was not found.");
        }
        String cityName = requireNonBlank(name, "City name");
        boolean isActive = active == null || active;
        Integer id = locationRepository.insertCity(stateId, countryId, cityName, isActive);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("name", cityName);
        result.put("stateId", stateId);
        result.put("active", isActive);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createArea(Integer cityId, String name, Boolean active) {
        if (cityId == null || cityId <= 0) {
            throw new IllegalArgumentException("City is required.");
        }
        Integer[] hierarchy = locationRepository.findCityHierarchyIds(cityId);
        if (hierarchy == null) {
            throw new IllegalArgumentException("Selected city was not found.");
        }
        String areaName = requireNonBlank(name, "Area name");
        boolean isActive = active == null || active;
        Integer id = locationRepository.insertArea(cityId, hierarchy[1], hierarchy[0], areaName, isActive);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("name", areaName);
        result.put("cityId", cityId);
        result.put("active", isActive);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createPincode(Integer areaId, String pincode, Boolean active) {
        if (areaId == null || areaId <= 0) {
            throw new IllegalArgumentException("Area is required.");
        }
        Integer[] hierarchy = locationRepository.findAreaHierarchyIds(areaId);
        if (hierarchy == null) {
            throw new IllegalArgumentException("Selected area was not found.");
        }
        String pin = requireNonBlank(pincode, "Pincode");
        boolean isActive = active == null || active;
        Integer id = locationRepository.insertPincode(hierarchy[0], hierarchy[1], hierarchy[2], areaId, pin, isActive);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("pincode", pin);
        result.put("name", pin);
        result.put("areaId", areaId);
        result.put("active", isActive);
        return result;
    }

    @Override
    @Transactional
    public void deleteCountry(Integer id) {
        locationRepository.deleteCountry(id);
    }

    @Override
    @Transactional
    public void deleteState(Integer id) {
        locationRepository.deleteState(id);
    }

    @Override
    @Transactional
    public void deleteCity(Integer id) {
        locationRepository.deleteCity(id);
    }

    @Override
    @Transactional
    public void deleteArea(Integer id) {
        locationRepository.deleteArea(id);
    }

    @Override
    @Transactional
    public void deletePincode(Integer id) {
        locationRepository.deletePincode(id);
    }

    @Override
    @Transactional
    public void updateCountry(Integer id, String name, String code, Boolean active) {
        String countryName = requireNonBlank(name, "Country name");
        String countryCode = requireNonBlank(code, "Country code");
        if (countryCode.length() != 2) {
            throw new IllegalArgumentException("Country code must be exactly 2 characters.");
        }
        locationRepository.updateCountry(id, countryName, countryCode, active == null || active);
    }

    @Override
    @Transactional
    public void updateState(Integer id, String name, Boolean active) {
        locationRepository.updateState(id, requireNonBlank(name, "State name"), active == null || active);
    }

    @Override
    @Transactional
    public void updateCity(Integer id, String name, Boolean active) {
        locationRepository.updateCity(id, requireNonBlank(name, "City name"), active == null || active);
    }

    @Override
    @Transactional
    public void updateArea(Integer id, String name, Boolean active) {
        locationRepository.updateArea(id, requireNonBlank(name, "Area name"), active == null || active);
    }

    @Override
    @Transactional
    public void updatePincode(Integer id, String pincode, Boolean active) {
        locationRepository.updatePincode(id, requireNonBlank(pincode, "Pincode"), active == null || active);
    }

    private boolean toActive(Object status) {
        if (status == null) {
            return true;
        }
        if (status instanceof Boolean bool) {
            return bool;
        }
        if (status instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = status.toString().trim().toLowerCase(Locale.ROOT);
        return !("0".equals(text) || "false".equals(text) || "inactive".equals(text));
    }

    /** Count helper — returns 0 for null (does not override BaseAdminService.toLong). */
    private long toCount(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private List<Map<String, Object>> mapRows(
            List<Object[]> rows,
            Function<Object[], Map<String, Object>> mapper) {
        List<Map<String, Object>> items = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            items.add(mapper.apply(row));
        }
        return items;
    }

    private PageResponse<Map<String, Object>> toPage(
            List<Map<String, Object>> items,
            long total,
            int page,
            int size) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(items, total, totalPages, page, size);
    }

    private int sanitizeSize(int size) {
        return Math.min(Math.max(size, 1), 5000);
    }
}
