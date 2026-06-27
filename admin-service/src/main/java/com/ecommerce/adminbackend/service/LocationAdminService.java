package com.ecommerce.adminbackend.service;

import java.util.List;
import java.util.Map;

public interface LocationAdminService {

    List<Map<String, Object>> listCountries(String search);

    List<Map<String, Object>> listStates(Integer countryId, String search);

    List<Map<String, Object>> listCities(Integer stateId, String search);

    List<Map<String, Object>> listAreas(Integer cityId, String search);

    List<Map<String, Object>> listPincodes(String search);

    Map<String, Long> getCounts();

    Map<String, Object> createCountry(String name);

    Map<String, Object> createState(Integer countryId, String name);

    Map<String, Object> createCity(Integer stateId, String name);

    Map<String, Object> createArea(Integer cityId, String name);

    Map<String, Object> createPincode(Integer areaId, String pincode);

    void deleteCountry(Integer id);

    void deleteState(Integer id);

    void deleteCity(Integer id);

    void deleteArea(Integer id);

    void deletePincode(Integer id);

    void updateCountry(Integer id, String name);

    void updateState(Integer id, String name);

    void updateCity(Integer id, String name);

    void updateArea(Integer id, String name);

    void updatePincode(Integer id, String pincode);
}
