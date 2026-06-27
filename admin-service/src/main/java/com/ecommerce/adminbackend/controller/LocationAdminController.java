package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import com.ecommerce.adminbackend.service.LocationAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/locations")
@RequiredArgsConstructor
public class LocationAdminController {

    private static final Logger log = LogFactory.getLogger(LocationAdminController.class);

    private final LocationAdminService locationAdminService;

    @GetMapping("/countries")
    public List<Map<String, Object>> countries(@RequestParam(required = false) String search) {
        return locationAdminService.listCountries(search);
    }

    @GetMapping("/states")
    public List<Map<String, Object>> states(
            @RequestParam(required = false) Integer countryId,
            @RequestParam(required = false) String search) {
        return locationAdminService.listStates(countryId, search);
    }

    @GetMapping("/cities")
    public List<Map<String, Object>> cities(
            @RequestParam(required = false) Integer stateId,
            @RequestParam(required = false) String search) {
        return locationAdminService.listCities(stateId, search);
    }

    @GetMapping("/areas")
    public List<Map<String, Object>> areas(
            @RequestParam(required = false) Integer cityId,
            @RequestParam(required = false) String search) {
        return locationAdminService.listAreas(cityId, search);
    }

    @GetMapping("/pincodes")
    public List<Map<String, Object>> pincodes(@RequestParam(required = false) String search) {
        return locationAdminService.listPincodes(search);
    }

    @GetMapping("/counts")
    public Map<String, Long> counts() {
        return locationAdminService.getCounts();
    }

    // CRUD operations
    @PostMapping("/countries")
    public Map<String, Object> createCountry(@RequestBody Map<String, String> request) {
        return locationAdminService.createCountry(request.get("name"));
    }

    @PostMapping("/states")
    public Map<String, Object> createState(@RequestBody Map<String, Object> request) {
        return locationAdminService.createState(
                (Integer) request.get("countryId"),
                (String) request.get("name")
        );
    }

    @PostMapping("/cities")
    public Map<String, Object> createCity(@RequestBody Map<String, Object> request) {
        return locationAdminService.createCity(
                (Integer) request.get("stateId"),
                (String) request.get("name")
        );
    }

    @PostMapping("/areas")
    public Map<String, Object> createArea(@RequestBody Map<String, Object> request) {
        return locationAdminService.createArea(
                (Integer) request.get("cityId"),
                (String) request.get("name")
        );
    }

    @PostMapping("/pincodes")
    public Map<String, Object> createPincode(@RequestBody Map<String, Object> request) {
        return locationAdminService.createPincode(
                (Integer) request.get("areaId"),
                (String) request.get("pincode")
        );
    }

    @PutMapping("/countries/{id}")
    public void updateCountry(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        locationAdminService.updateCountry(id, request.get("name"));
    }

    @PutMapping("/states/{id}")
    public void updateState(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        locationAdminService.updateState(id, request.get("name"));
    }

    @PutMapping("/cities/{id}")
    public void updateCity(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        locationAdminService.updateCity(id, request.get("name"));
    }

    @PutMapping("/areas/{id}")
    public void updateArea(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        locationAdminService.updateArea(id, request.get("name"));
    }

    @PutMapping("/pincodes/{id}")
    public void updatePincode(@PathVariable Integer id, @RequestBody Map<String, String> request) {
        locationAdminService.updatePincode(id, request.get("pincode"));
    }

    @DeleteMapping("/countries/{id}")
    public void deleteCountry(@PathVariable Integer id) {
        locationAdminService.deleteCountry(id);
    }

    @DeleteMapping("/states/{id}")
    public void deleteState(@PathVariable Integer id) {
        locationAdminService.deleteState(id);
    }

    @DeleteMapping("/cities/{id}")
    public void deleteCity(@PathVariable Integer id) {
        locationAdminService.deleteCity(id);
    }

    @DeleteMapping("/areas/{id}")
    public void deleteArea(@PathVariable Integer id) {
        locationAdminService.deleteArea(id);
    }

    @DeleteMapping("/pincodes/{id}")
    public void deletePincode(@PathVariable Integer id) {
        locationAdminService.deletePincode(id);
    }
}
