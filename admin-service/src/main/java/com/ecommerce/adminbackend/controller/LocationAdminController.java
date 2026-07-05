package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import com.ecommerce.adminbackend.service.LocationAdminService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/countries")
    public Map<String, Object> createCountry(@RequestBody Map<String, Object> request) {
        return locationAdminService.createCountry(
                stringValue(request.get("name")),
                stringValue(request.get("code")),
                boolValue(request.get("status"), request.get("active")));
    }

    @PostMapping("/states")
    public Map<String, Object> createState(@RequestBody Map<String, Object> request) {
        return locationAdminService.createState(
                intValue(request.get("countryId")),
                stringValue(request.get("name")),
                boolValue(request.get("status"), request.get("active")));
    }

    @PostMapping("/cities")
    public Map<String, Object> createCity(@RequestBody Map<String, Object> request) {
        return locationAdminService.createCity(
                intValue(request.get("stateId")),
                stringValue(request.get("name")),
                boolValue(request.get("status"), request.get("active")));
    }

    @PostMapping("/areas")
    public Map<String, Object> createArea(@RequestBody Map<String, Object> request) {
        return locationAdminService.createArea(
                intValue(request.get("cityId")),
                stringValue(request.get("name")),
                boolValue(request.get("status"), request.get("active")));
    }

    @PostMapping("/pincodes")
    public Map<String, Object> createPincode(@RequestBody Map<String, Object> request) {
        return locationAdminService.createPincode(
                intValue(request.get("areaId")),
                stringValue(request.get("pincode"), request.get("name")),
                boolValue(request.get("status"), request.get("active")));
    }

    @PutMapping("/countries/{id}")
    public ResponseEntity<Map<String, String>> updateCountry(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request) {
        locationAdminService.updateCountry(
                id,
                stringValue(request.get("name")),
                stringValue(request.get("code")),
                boolValue(request.get("status"), request.get("active")));
        return ResponseEntity.ok(Map.of("message", "Country updated."));
    }

    @PutMapping("/states/{id}")
    public ResponseEntity<Map<String, String>> updateState(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request) {
        locationAdminService.updateState(
                id,
                stringValue(request.get("name")),
                boolValue(request.get("status"), request.get("active")));
        return ResponseEntity.ok(Map.of("message", "State updated."));
    }

    @PutMapping("/cities/{id}")
    public ResponseEntity<Map<String, String>> updateCity(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request) {
        locationAdminService.updateCity(
                id,
                stringValue(request.get("name")),
                boolValue(request.get("status"), request.get("active")));
        return ResponseEntity.ok(Map.of("message", "City updated."));
    }

    @PutMapping("/areas/{id}")
    public ResponseEntity<Map<String, String>> updateArea(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request) {
        locationAdminService.updateArea(
                id,
                stringValue(request.get("name")),
                boolValue(request.get("status"), request.get("active")));
        return ResponseEntity.ok(Map.of("message", "Area updated."));
    }

    @PutMapping("/pincodes/{id}")
    public ResponseEntity<Map<String, String>> updatePincode(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request) {
        locationAdminService.updatePincode(
                id,
                stringValue(request.get("pincode"), request.get("name")),
                boolValue(request.get("status"), request.get("active")));
        return ResponseEntity.ok(Map.of("message", "Pincode updated."));
    }

    @DeleteMapping("/countries/{id}")
    public ResponseEntity<Map<String, String>> deleteCountry(@PathVariable Integer id) {
        locationAdminService.deleteCountry(id);
        return ResponseEntity.ok(Map.of("message", "Country deleted."));
    }

    @DeleteMapping("/states/{id}")
    public ResponseEntity<Map<String, String>> deleteState(@PathVariable Integer id) {
        locationAdminService.deleteState(id);
        return ResponseEntity.ok(Map.of("message", "State deleted."));
    }

    @DeleteMapping("/cities/{id}")
    public ResponseEntity<Map<String, String>> deleteCity(@PathVariable Integer id) {
        locationAdminService.deleteCity(id);
        return ResponseEntity.ok(Map.of("message", "City deleted."));
    }

    @DeleteMapping("/areas/{id}")
    public ResponseEntity<Map<String, String>> deleteArea(@PathVariable Integer id) {
        locationAdminService.deleteArea(id);
        return ResponseEntity.ok(Map.of("message", "Area deleted."));
    }

    @DeleteMapping("/pincodes/{id}")
    public ResponseEntity<Map<String, String>> deletePincode(@PathVariable Integer id) {
        locationAdminService.deletePincode(id);
        return ResponseEntity.ok(Map.of("message", "Pincode deleted."));
    }

    private static String stringValue(Object... values) {
        for (Object value : values) {
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return null;
    }

    private static Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        return Integer.parseInt(text);
    }

    private static Boolean boolValue(Object status, Object active) {
        Object value = active != null ? active : status;
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = value.toString().trim().toLowerCase();
        if ("active".equals(text) || "true".equals(text) || "1".equals(text)) {
            return true;
        }
        if ("inactive".equals(text) || "false".equals(text) || "0".equals(text)) {
            return false;
        }
        return null;
    }
}
