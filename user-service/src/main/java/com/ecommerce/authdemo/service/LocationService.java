package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.CityRequest;
import com.ecommerce.authdemo.dto.CityResponse;
import com.ecommerce.authdemo.dto.CountryRequest;
import com.ecommerce.authdemo.dto.CountryResponse;
import com.ecommerce.authdemo.dto.LocationRequest;
import com.ecommerce.authdemo.dto.PincodeRequest;
import com.ecommerce.authdemo.dto.PincodeResponse;
import com.ecommerce.authdemo.dto.StateRequest;
import com.ecommerce.authdemo.dto.StateResponse;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public interface LocationService {

    void saveLocation(LocationRequest request);

    CityResponse createCity(CityRequest request);

    List<CityResponse> getCities(Integer countryId, Integer stateId, Boolean status);

    CityResponse updateCityStatus(Integer cityId, Boolean status);

    CountryResponse createCountry(CountryRequest request);

    List<CountryResponse> getCountries(Boolean status);

    CountryResponse updateCountryStatus(Integer countryId, Boolean status);

    StateResponse createState(StateRequest request);

    List<StateResponse> getStates(Integer countryId, Boolean status);

    StateResponse updateStateStatus(Integer stateId, Boolean status);

    PincodeResponse createPincode(PincodeRequest request);

    List<PincodeResponse> getPincodes(Integer countryId,
                                      Integer stateId,
                                      Integer cityId,
                                      Integer areaId,
                                      String pincode,
                                      Boolean status);

    PincodeResponse updatePincodeStatus(Integer pincodeId, Boolean status);

}