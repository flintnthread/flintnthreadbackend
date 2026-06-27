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
    public Map<String, Object> createCountry(String name) {
        Integer id = locationRepository.insertCountry(name);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("name", name);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createState(Integer countryId, String name) {
        Integer id = locationRepository.insertState(countryId, name);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("name", name);
        result.put("countryId", countryId);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createCity(Integer stateId, String name) {
        Integer id = locationRepository.insertCity(stateId, name);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("name", name);
        result.put("stateId", stateId);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createArea(Integer cityId, String name) {
        Integer id = locationRepository.insertArea(cityId, name);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("name", name);
        result.put("cityId", cityId);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createPincode(Integer areaId, String pincode) {
        Integer id = locationRepository.insertPincode(areaId, pincode);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("pincode", pincode);
        result.put("areaId", areaId);
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
    public void updateCountry(Integer id, String name) {
        locationRepository.updateCountry(id, name);
    }

    @Override
    @Transactional
    public void updateState(Integer id, String name) {
        locationRepository.updateState(id, name);
    }

    @Override
    @Transactional
    public void updateCity(Integer id, String name) {
        locationRepository.updateCity(id, name);
    }

    @Override
    @Transactional
    public void updateArea(Integer id, String name) {
        locationRepository.updateArea(id, name);
    }

    @Override
    @Transactional
    public void updatePincode(Integer id, String pincode) {
        locationRepository.updatePincode(id, pincode);
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
