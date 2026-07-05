package com.ecommerce.adminbackend.service.impl;

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

@Service
@RequiredArgsConstructor
public class LocationAdminServiceImpl extends BaseAdminService implements LocationAdminService {

    private final LocationRepository locationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCountries(String search) {
        return mapRows(locationRepository.searchCountries(blankToNull(search)), row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("name", row[1]);
            item.put("code", row[2]);
            item.put("active", toActive(row[3]));
            return item;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listStates(Integer countryId, String search) {
        return mapRows(locationRepository.searchStates(countryId, blankToNull(search)), row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("name", row[1]);
            item.put("countryId", row[2]);
            item.put("countryName", row[3]);
            item.put("active", toActive(row[4]));
            return item;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCities(Integer stateId, String search) {
        return mapRows(locationRepository.searchCities(stateId, blankToNull(search)), row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("name", row[1]);
            item.put("stateId", row[2]);
            item.put("stateName", row[3]);
            item.put("active", toActive(row[4]));
            return item;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAreas(Integer cityId, String search) {
        return mapRows(locationRepository.searchAreas(cityId, blankToNull(search)), row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("name", row[1]);
            item.put("cityId", row[2]);
            item.put("cityName", row[3]);
            item.put("active", toActive(row[4]));
            return item;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPincodes(String search) {
        return mapRows(locationRepository.searchPincodes(blankToNull(search)), row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("pincode", row[1]);
            item.put("area", row[2]);
            item.put("city", row[3]);
            item.put("state", row[4]);
            item.put("country", row[5]);
            item.put("name", row[1]);
            return item;
        });
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

    private List<Map<String, Object>> mapRows(
            List<Object[]> rows,
            java.util.function.Function<Object[], Map<String, Object>> mapper) {
        List<Map<String, Object>> items = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            items.add(mapper.apply(row));
        }
        return items;
    }
}
