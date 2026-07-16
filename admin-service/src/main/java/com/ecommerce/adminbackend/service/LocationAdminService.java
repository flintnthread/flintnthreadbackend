package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.common.PageResponse;

import java.util.Map;

public interface LocationAdminService {

    PageResponse<Map<String, Object>> listCountries(String search, int page, int size);

    PageResponse<Map<String, Object>> listStates(Integer countryId, String search, int page, int size);

    PageResponse<Map<String, Object>> listCities(Integer stateId, String search, int page, int size);

    PageResponse<Map<String, Object>> listAreas(Integer cityId, String search, int page, int size);

    PageResponse<Map<String, Object>> listPincodes(Integer cityId, Integer areaId, String search, int page, int size);

    Map<String, Long> getCounts();

    Map<String, Object> createCountry(String name, String code, Boolean active);

    Map<String, Object> createState(Integer countryId, String name, Boolean active);

    Map<String, Object> createCity(Integer stateId, String name, Boolean active);

    Map<String, Object> createArea(Integer cityId, String name, Boolean active);

    Map<String, Object> createPincode(Integer areaId, String pincode, Boolean active);

    void deleteCountry(Integer id);

    void deleteState(Integer id);

    void deleteCity(Integer id);

    void deleteArea(Integer id);

    void deletePincode(Integer id);

    void updateCountry(Integer id, String name, String code, Boolean active);

    void updateState(Integer id, String name, Boolean active);

    void updateCity(Integer id, String name, Boolean active);

    void updateArea(Integer id, String name, Boolean active);

    void updatePincode(Integer id, String pincode, Boolean active);
}
