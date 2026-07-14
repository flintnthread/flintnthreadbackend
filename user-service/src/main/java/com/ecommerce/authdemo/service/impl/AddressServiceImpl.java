package com.ecommerce.authdemo.service.impl;


import com.ecommerce.authdemo.dto.AddressRequest;
import com.ecommerce.authdemo.entity.Address;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.repository.AddressRepository;
import com.ecommerce.authdemo.repository.OrderRepository;
import com.ecommerce.authdemo.service.AddressService;
import com.ecommerce.authdemo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.ecommerce.authdemo.repository.DeliveryPincodeRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {


    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final SecurityUtil securityUtil;
    private final DeliveryPincodeRepository deliveryPincodeRepository;

    private Long getUserId() {
        return securityUtil.getCurrentUserId();
    }

    private User getCurrentUser() {
        return securityUtil.getCurrentUser();
    }

    @Override
    @Transactional
    public Address addAddress(AddressRequest request) {

        Long userId = getUserId();

        boolean isFirst = addressRepository.findByUserId(userId).isEmpty();

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefault(userId);
        }

        if (request.getLatitude() != null && request.getLongitude() != null) {

            Map<String, String> location =
                    getAddressFromLatLng(request.getLatitude(), request.getLongitude());

            request.setAddressLine1(location.get("fullAddress"));
            request.setCity(location.get("city"));
            request.setState(location.get("state"));
            request.setCountry(location.get("country"));
            request.setPincode(location.get("pincode"));

        }

        String pincode = request.getPincode() != null ? request.getPincode().trim() : "";

        if (!pincode.matches("^[0-9]{6}$")) {
            throw new IllegalArgumentException("Please enter a valid 6-digit pincode.");
        }

        boolean deliveryAvailable = deliveryPincodeRepository.existsByPincodeAndStatus(pincode, 1);

        if (!deliveryAvailable) {
            throw new IllegalArgumentException("Delivery not available for this pincode.");
        }

        request.setPincode(pincode);

        if (request.getCity() == null || request.getCity().trim().isEmpty()) {
            throw new IllegalArgumentException("City is required");
        }

        User user = getCurrentUser();

        Address address = Address.builder()
                .userId(userId)
                .name(request.getName() != null ? request.getName() : "Current Location")
                .email(request.getEmail() != null ? request.getEmail() : user.getEmail())
                .phone(request.getPhone() != null ? request.getPhone() : user.getContactNumber())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .pincode(request.getPincode())
                .addressType(request.getAddressType() != null ? request.getAddressType() : "current")
                .isDefault(isFirst || Boolean.TRUE.equals(request.getIsDefault()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return addressRepository.save(address);
    }

    private Map<String, String> getAddressFromLatLng(Double lat, Double lng) {

        Map<String, String> result = new HashMap<>();

        try {
            String url = "https://nominatim.openstreetmap.org/reverse?format=json&lat="
                    + lat + "&lon=" + lng;

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "ecommerce-app");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getBody() != null) {

                Map body = response.getBody();

                result.put("fullAddress", body.get("display_name") != null
                        ? body.get("display_name").toString()
                        : "Current Location");

                Map address = (Map) body.get("address");

                if (address != null) {
                    result.put("city", getSafe(address, "city", "town", "village"));
                    result.put("state", getSafe(address, "state"));
                    result.put("country", getSafe(address, "country"));
                    result.put("pincode", getSafe(address, "postcode"));
                }
            }

        } catch (Exception e) {
            System.out.println("Geolocation error: " + e.getMessage());
        }

        result.putIfAbsent("fullAddress", "Current Location");
        result.putIfAbsent("city", "Unknown City");
        result.putIfAbsent("state", "Unknown State");
        result.putIfAbsent("country", "India");
        result.putIfAbsent("pincode", "000000");

        return result;
    }

    private String getSafe(Map map, String... keys) {
        for (String key : keys) {
            if (map.get(key) != null) {
                return map.get(key).toString();
            }
        }
        return "Unknown";
    }

    @Override
    public List<Address> getUserAddresses() {
        return addressRepository.findByUserId(getUserId());
    }

    @Override
    @Transactional
    public Address updateAddress(Integer id, AddressRequest request) {

        Long userId = getUserId();

        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefault(userId);
        }

        address.setName(request.getName());
        address.setEmail(request.getEmail());
        address.setPhone(request.getPhone());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setCountry(request.getCountry());

        String pincode = request.getPincode() != null ? request.getPincode().trim() : "";

        if (!pincode.matches("^[0-9]{6}$")) {
            throw new IllegalArgumentException("Please enter a valid 6-digit pincode.");
        }

        boolean deliveryAvailable = deliveryPincodeRepository.existsByPincodeAndStatus(pincode, 1);

        if (!deliveryAvailable) {
            throw new IllegalArgumentException("Delivery not available for this pincode.");
        }

        address.setPincode(pincode);

        address.setAddressType(request.getAddressType());

        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }

        address.setUpdatedAt(LocalDateTime.now());

        return addressRepository.save(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Integer id) {
        Long userId = getUserId();

        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());

        // Detach from orders first so FK / address_id references do not block delete
        orderRepository.clearAddressId(Long.valueOf(id));

        addressRepository.delete(address);
        addressRepository.flush();

        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserId(userId);
            if (!remaining.isEmpty()) {
                Address nextDefault = remaining.get(0);
                nextDefault.setIsDefault(true);
                nextDefault.setUpdatedAt(LocalDateTime.now());
                addressRepository.save(nextDefault);
            }
        }
    }

    @Override
    @Transactional
    public Address setDefaultAddress(Integer addressId) {

        Long userId = getUserId();

        clearDefault(userId);

        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        address.setIsDefault(true);

        return addressRepository.save(address);
    }

    @Override
    public Address getDefaultAddress() {
        return addressRepository.findByUserIdAndIsDefaultTrue(getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Default address not found"));
    }

    @Override
    @Transactional
    public void deleteAllForCurrentUser() {
        Long userId = getUserId();
        orderRepository.clearAddressIdsForUser(userId);
        addressRepository.deleteByUserId(userId);
    }

    private void clearDefault(Long userId) {
        List<Address> addresses = addressRepository.findByUserId(userId);
        for (Address a : addresses) {
            a.setIsDefault(false);
        }
        addressRepository.saveAll(addresses);
    }
}