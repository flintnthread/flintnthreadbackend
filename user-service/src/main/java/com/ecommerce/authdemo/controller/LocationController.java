package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ApiResponse;
import com.ecommerce.authdemo.dto.CityRequest;
import com.ecommerce.authdemo.dto.CityResponse;
import com.ecommerce.authdemo.dto.CityStatusUpdateRequest;
import com.ecommerce.authdemo.dto.CountryRequest;
import com.ecommerce.authdemo.dto.CountryResponse;
import com.ecommerce.authdemo.dto.CountryStatusUpdateRequest;
import com.ecommerce.authdemo.dto.LocationRequest;
import com.ecommerce.authdemo.dto.PincodeRequest;
import com.ecommerce.authdemo.dto.PincodeResponse;
import com.ecommerce.authdemo.dto.PincodeStatusUpdateRequest;
import com.ecommerce.authdemo.dto.StateRequest;
import com.ecommerce.authdemo.dto.StateResponse;
import com.ecommerce.authdemo.dto.StateStatusUpdateRequest;
import com.ecommerce.authdemo.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping("/update")
    public String updateLocation(@RequestBody LocationRequest request) {

        locationService.saveLocation(request);

        return "Location saved successfully";

    }

    @PostMapping("/cities")
    public ResponseEntity<ApiResponse<CityResponse>> createCity(
            @Valid @RequestBody CityRequest request) {
        CityResponse city = locationService.createCity(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "City created successfully", city));
    }

    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<List<CityResponse>>> getCities(
            @RequestParam(required = false) Integer countryId,
            @RequestParam(required = false) Integer stateId,
            @RequestParam(required = false) Boolean status) {
        List<CityResponse> cities = locationService.getCities(countryId, stateId, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cities fetched successfully", cities));
    }

    @PatchMapping("/cities/{id}/status")
    public ResponseEntity<ApiResponse<CityResponse>> updateCityStatus(
            @PathVariable Integer id,
            @Valid @RequestBody CityStatusUpdateRequest request) {
        CityResponse city = locationService.updateCityStatus(id, request.getStatus());
        return ResponseEntity.ok(new ApiResponse<>(true, "City status updated successfully", city));
    }

    @PostMapping("/countries")
    public ResponseEntity<ApiResponse<CountryResponse>> createCountry(
            @Valid @RequestBody CountryRequest request) {
        CountryResponse country = locationService.createCountry(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Country created successfully", country));
    }

    @GetMapping("/countries")
    public ResponseEntity<ApiResponse<List<CountryResponse>>> getCountries(
            @RequestParam(required = false) Boolean status) {
        List<CountryResponse> countries = locationService.getCountries(status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Countries fetched successfully", countries));
    }

    @PatchMapping("/countries/{id}/status")
    public ResponseEntity<ApiResponse<CountryResponse>> updateCountryStatus(
            @PathVariable Integer id,
            @Valid @RequestBody CountryStatusUpdateRequest request) {
        CountryResponse country = locationService.updateCountryStatus(id, request.getStatus());
        return ResponseEntity.ok(new ApiResponse<>(true, "Country status updated successfully", country));
    }

    @PostMapping("/states")
    public ResponseEntity<ApiResponse<StateResponse>> createState(
            @Valid @RequestBody StateRequest request) {
        StateResponse state = locationService.createState(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "State created successfully", state));
    }

    @GetMapping("/states")
    public ResponseEntity<ApiResponse<List<StateResponse>>> getStates(
            @RequestParam(required = false) Integer countryId,
            @RequestParam(required = false) Boolean status) {
        List<StateResponse> states = locationService.getStates(countryId, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "States fetched successfully", states));
    }

    @PatchMapping("/states/{id}/status")
    public ResponseEntity<ApiResponse<StateResponse>> updateStateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody StateStatusUpdateRequest request) {
        StateResponse state = locationService.updateStateStatus(id, request.getStatus());
        return ResponseEntity.ok(new ApiResponse<>(true, "State status updated successfully", state));
    }

    @PostMapping("/pincodes")
    public ResponseEntity<ApiResponse<PincodeResponse>> createPincode(
            @Valid @RequestBody PincodeRequest request) {
        PincodeResponse pincode = locationService.createPincode(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Pincode created successfully", pincode));
    }

    @GetMapping("/pincodes")
    public ResponseEntity<ApiResponse<List<PincodeResponse>>> getPincodes(
            @RequestParam(required = false) Integer countryId,
            @RequestParam(required = false) Integer stateId,
            @RequestParam(required = false) Integer cityId,
            @RequestParam(required = false) Integer areaId,
            @RequestParam(required = false) String pincode,
            @RequestParam(required = false) Boolean status) {
        List<PincodeResponse> pincodes = locationService.getPincodes(
                countryId, stateId, cityId, areaId, pincode, status
        );
        return ResponseEntity.ok(new ApiResponse<>(true, "Pincodes fetched successfully", pincodes));
    }

    @PatchMapping("/pincodes/{id}/status")
    public ResponseEntity<ApiResponse<PincodeResponse>> updatePincodeStatus(
            @PathVariable Integer id,
            @Valid @RequestBody PincodeStatusUpdateRequest request) {
        PincodeResponse pincode = locationService.updatePincodeStatus(id, request.getStatus());
        return ResponseEntity.ok(new ApiResponse<>(true, "Pincode status updated successfully", pincode));
    }
}