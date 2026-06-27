package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.CityRequest;
import com.ecommerce.authdemo.dto.CityResponse;
import com.ecommerce.authdemo.dto.CountryRequest;
import com.ecommerce.authdemo.dto.CountryResponse;
import com.ecommerce.authdemo.dto.LocationRequest;
import com.ecommerce.authdemo.dto.PincodeRequest;
import com.ecommerce.authdemo.dto.PincodeResponse;
import com.ecommerce.authdemo.dto.StateRequest;
import com.ecommerce.authdemo.dto.StateResponse;
import com.ecommerce.authdemo.entity.City;
import com.ecommerce.authdemo.entity.Country;
import com.ecommerce.authdemo.entity.Pincode;
import com.ecommerce.authdemo.entity.State;
import com.ecommerce.authdemo.entity.UserLocation;
import com.ecommerce.authdemo.repository.CityRepository;
import com.ecommerce.authdemo.repository.CountryRepository;
import com.ecommerce.authdemo.repository.PincodeRepository;
import com.ecommerce.authdemo.repository.StateRepository;
import com.ecommerce.authdemo.repository.UserLocationRepository;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final UserLocationRepository userLocationRepository;
    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final PincodeRepository pincodeRepository;

    @Override
    public void saveLocation(LocationRequest request) {

        UserLocation location = new UserLocation();

        location.setUserId(request.getUserId());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setCreatedAt(LocalDateTime.now());

        userLocationRepository.save(location);
    }

    @Override
    public CityResponse createCity(CityRequest request) {
        City city = City.builder()
                .cityName(request.getCityName().trim())
                .stateId(request.getStateId())
                .countryId(request.getCountryId())
                .status(request.getStatus() != null ? request.getStatus() : Boolean.TRUE)
                .build();

        City saved = cityRepository.save(city);
        return toResponse(saved);
    }

    @Override
    public List<CityResponse> getCities(Integer countryId, Integer stateId, Boolean status) {
        return cityRepository.findWithFilters(countryId, stateId, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CityResponse updateCityStatus(Integer cityId, Boolean status) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found"));

        city.setStatus(status);
        City updated = cityRepository.save(city);
        return toResponse(updated);
    }

    @Override
    public CountryResponse createCountry(CountryRequest request) {
        Country country = Country.builder()
                .countryName(request.getCountryName().trim())
                .countryCode(request.getCountryCode().trim().toUpperCase())
                .status(request.getStatus() != null ? request.getStatus() : Boolean.TRUE)
                .build();

        Country saved = countryRepository.save(country);
        return toResponse(saved);
    }

    @Override
    public List<CountryResponse> getCountries(Boolean status) {
        return countryRepository.findWithStatus(status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CountryResponse updateCountryStatus(Integer countryId, Boolean status) {
        Country country = countryRepository.findById(countryId)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found"));

        country.setStatus(status);
        Country updated = countryRepository.save(country);
        return toResponse(updated);
    }

    @Override
    public StateResponse createState(StateRequest request) {
        State state = State.builder()
                .stateName(request.getStateName().trim())
                .countryId(request.getCountryId())
                .status(request.getStatus() != null ? request.getStatus() : Boolean.TRUE)
                .build();

        State saved = stateRepository.save(state);
        return toResponse(saved);
    }

    @Override
    public List<StateResponse> getStates(Integer countryId, Boolean status) {
        return stateRepository.findWithFilters(countryId, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public StateResponse updateStateStatus(Integer stateId, Boolean status) {
        State state = stateRepository.findById(stateId)
                .orElseThrow(() -> new ResourceNotFoundException("State not found"));

        state.setStatus(status);
        State updated = stateRepository.save(state);
        return toResponse(updated);
    }

    @Override
    public PincodeResponse createPincode(PincodeRequest request) {
        Pincode pincode = Pincode.builder()
                .countryId(request.getCountryId())
                .stateId(request.getStateId())
                .cityId(request.getCityId())
                .areaId(request.getAreaId())
                .pincode(request.getPincode().trim())
                .status(request.getStatus() != null ? request.getStatus() : Boolean.TRUE)
                .build();

        Pincode saved = pincodeRepository.save(pincode);
        return toResponse(saved);
    }

    @Override
    public List<PincodeResponse> getPincodes(Integer countryId,
                                             Integer stateId,
                                             Integer cityId,
                                             Integer areaId,
                                             String pincode,
                                             Boolean status) {
        return pincodeRepository
                .findWithFilters(countryId, stateId, cityId, areaId, status, normalize(pincode))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PincodeResponse updatePincodeStatus(Integer pincodeId, Boolean status) {
        Pincode pincode = pincodeRepository.findById(pincodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Pincode not found"));

        pincode.setStatus(status);
        Pincode updated = pincodeRepository.save(pincode);
        return toResponse(updated);
    }

    private CityResponse toResponse(City city) {
        return CityResponse.builder()
                .id(city.getId())
                .cityName(city.getCityName())
                .stateId(city.getStateId())
                .countryId(city.getCountryId())
                .status(city.getStatus())
                .createdAt(city.getCreatedAt())
                .build();
    }

    private CountryResponse toResponse(Country country) {
        return CountryResponse.builder()
                .id(country.getId())
                .countryName(country.getCountryName())
                .countryCode(country.getCountryCode())
                .status(country.getStatus())
                .createdAt(country.getCreatedAt())
                .build();
    }

    private StateResponse toResponse(State state) {
        return StateResponse.builder()
                .id(state.getId())
                .stateName(state.getStateName())
                .countryId(state.getCountryId())
                .status(state.getStatus())
                .createdAt(state.getCreatedAt())
                .build();
    }

    private PincodeResponse toResponse(Pincode pincode) {
        return PincodeResponse.builder()
                .id(pincode.getId())
                .countryId(pincode.getCountryId())
                .stateId(pincode.getStateId())
                .cityId(pincode.getCityId())
                .areaId(pincode.getAreaId())
                .pincode(pincode.getPincode())
                .status(pincode.getStatus())
                .createdAt(pincode.getCreatedAt())
                .updatedAt(pincode.getUpdatedAt())
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}