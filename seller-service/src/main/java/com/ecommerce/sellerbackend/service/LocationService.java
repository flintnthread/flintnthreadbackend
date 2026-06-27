package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.AddPincodeRequest;
import com.ecommerce.sellerbackend.dto.CreateLocationRequest;
import com.ecommerce.sellerbackend.dto.LocationItemResponse;
import com.ecommerce.sellerbackend.dto.LocationSearchResponse;

import java.util.List;

public interface LocationService {

    List<LocationItemResponse> listCountries(String search);

    List<LocationItemResponse> listStates(Integer countryId, String search);

    List<LocationItemResponse> listCities(Integer stateId, String search);

    List<LocationItemResponse> listAreas(Integer cityId, String search);

    List<LocationItemResponse> listPincodes(Integer areaId, String search);

    List<LocationSearchResponse> searchLocations(String search, String country);

    LocationSearchResponse enrichExternalLocation(LocationSearchResponse item);

    LocationSearchResponse saveLocation(CreateLocationRequest request);

    LocationItemResponse addPincode(AddPincodeRequest request);

    LocationSearchResponse resolveLocation(String country, String state, String city, String area, String pincode);

    LocationSearchResponse resolvePlace(double latitude, double longitude);
}
