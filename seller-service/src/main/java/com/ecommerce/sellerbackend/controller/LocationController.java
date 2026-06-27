package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.AddPincodeRequest;
import com.ecommerce.sellerbackend.dto.CreateLocationRequest;
import com.ecommerce.sellerbackend.dto.LocationItemResponse;
import com.ecommerce.sellerbackend.dto.LocationSearchResponse;
import com.ecommerce.sellerbackend.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/countries")
    public List<LocationItemResponse> countries(@RequestParam(required = false) String search) {
        return locationService.listCountries(search);
    }

    @GetMapping("/states")
    public List<LocationItemResponse> states(
            @RequestParam(required = false) Integer countryId,
            @RequestParam(required = false) String search) {
        return locationService.listStates(countryId, search);
    }

    @GetMapping("/cities")
    public List<LocationItemResponse> cities(
            @RequestParam(required = false) Integer stateId,
            @RequestParam(required = false) String search) {
        return locationService.listCities(stateId, search);
    }

    @GetMapping("/areas")
    public List<LocationItemResponse> areas(
            @RequestParam(required = false) Integer cityId,
            @RequestParam(required = false) String search) {
        return locationService.listAreas(cityId, search);
    }

    @GetMapping("/pincodes")
    public List<LocationItemResponse> pincodes(
            @RequestParam(required = false) Integer areaId,
            @RequestParam(required = false) String search) {
        return locationService.listPincodes(areaId, search);
    }

    @GetMapping("/search")
    public List<LocationSearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String country) {
        return locationService.searchLocations(q, country);
    }

    @GetMapping("/resolve")
    public LocationSearchResponse resolve(
            @RequestParam String country,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String pincode) {
        return locationService.resolveLocation(country, state, city, area, pincode);
    }

    @PostMapping("/enrich")
    public LocationSearchResponse enrich(@RequestBody LocationSearchResponse item) {
        return locationService.enrichExternalLocation(item);
    }

    /** Google Maps-style place details — reverse geocode at suggestion coordinates. */
    @GetMapping("/place")
    public LocationSearchResponse resolvePlace(
            @RequestParam double lat,
            @RequestParam double lon) {
        return locationService.resolvePlace(lat, lon);
    }

    @PostMapping
    public LocationSearchResponse saveLocation(@Valid @RequestBody CreateLocationRequest request) {
        return locationService.saveLocation(request);
    }

    @PostMapping("/pincodes")
    public LocationItemResponse addPincode(@Valid @RequestBody AddPincodeRequest request) {
        return locationService.addPincode(request);
    }
}
