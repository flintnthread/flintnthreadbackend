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

    /** ~100m — treat GPS points this close as the same delivery location. */
    private static final double SAME_LOCATION_DEGREES = 0.0009;

    @Override
    @Transactional
    public Address addAddress(AddressRequest request) {

        Long userId = getUserId();
        List<Address> existing = addressRepository.findByUserId(userId);
        boolean isFirst = existing.isEmpty();

        // Only fill missing fields from GPS — never overwrite a client-provided address
        if (request.getLatitude() != null && request.getLongitude() != null) {
            Map<String, String> location =
                    getAddressFromLatLng(request.getLatitude(), request.getLongitude());

            if (isBlank(request.getAddressLine1())) {
                request.setAddressLine1(location.get("fullAddress"));
            }
            if (isBlank(request.getCity())) {
                request.setCity(location.get("city"));
            }
            if (isBlank(request.getState())) {
                request.setState(location.get("state"));
            }
            if (isBlank(request.getCountry())) {
                request.setCountry(location.get("country"));
            }
            if (isBlank(request.getPincode())
                    || !String.valueOf(request.getPincode()).trim().matches("^[0-9]{6}$")) {
                request.setPincode(location.get("pincode"));
            }
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

        // Same address already saved for this user → reuse (no duplicate rows)
        Address duplicate = findDuplicateAddress(existing, request);
        if (duplicate != null) {
            return reuseExistingAddress(duplicate, request, userId);
        }

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefault(userId);
        }

        User user = getCurrentUser();

String phone = request.getPhone();

if (phone == null || phone.trim().isEmpty()) {
    phone = user.getContactNumber();
}

if (phone == null || phone.trim().isEmpty()) {
    throw new IllegalArgumentException("Phone number is required");
}

Address address = Address.builder()
        .userId(userId)
        .name(request.getName() != null ? request.getName() : "Current Location")
        .email(request.getEmail() != null ? request.getEmail() : user.getEmail())
        .phone(phone)
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry() != null ? request.getCountry() : "India")
                .pincode(request.getPincode())
                .addressType(request.getAddressType() != null ? request.getAddressType() : "home")
                .isDefault(isFirst || Boolean.TRUE.equals(request.getIsDefault()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return addressRepository.save(address);
    }

    private Address reuseExistingAddress(Address existing, AddressRequest request, Long userId) {
        boolean dirty = false;

        if (request.getLatitude() != null && request.getLongitude() != null) {
            existing.setLatitude(request.getLatitude());
            existing.setLongitude(request.getLongitude());
            dirty = true;
        }
        if (!isBlank(request.getPhone()) && isBlank(existing.getPhone())) {
            existing.setPhone(request.getPhone().trim());
            dirty = true;
        }
        if (!isBlank(request.getEmail()) && isBlank(existing.getEmail())) {
            existing.setEmail(request.getEmail().trim());
            dirty = true;
        }
        if (!isBlank(request.getName()) && isBlank(existing.getName())) {
            existing.setName(request.getName().trim());
            dirty = true;
        }

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(existing.getIsDefault())) {
            clearDefault(userId);
            existing.setIsDefault(true);
            dirty = true;
        }

        existing.setUpdatedAt(LocalDateTime.now());
        return dirty ? addressRepository.save(existing) : existing;
    }

    private Address findDuplicateAddress(List<Address> existing, AddressRequest request) {
        if (existing == null || existing.isEmpty()) {
            return null;
        }

        String wantedPin = normalizePincode(request.getPincode());
        String wantedLine = normalizeAddressKey(request.getAddressLine1(), request.getAddressLine2());
        String wantedCity = normalizeKey(request.getCity());
        Double lat = request.getLatitude();
        Double lng = request.getLongitude();

        for (Address row : existing) {
            String rowPin = normalizePincode(row.getPincode());
            if (!wantedPin.isEmpty() && !wantedPin.equals(rowPin)) {
                continue;
            }

            // Same GPS spot (current location / map pin)
            if (lat != null && lng != null
                    && row.getLatitude() != null && row.getLongitude() != null
                    && Math.abs(lat - row.getLatitude()) <= SAME_LOCATION_DEGREES
                    && Math.abs(lng - row.getLongitude()) <= SAME_LOCATION_DEGREES) {
                return row;
            }

            String rowLine = normalizeAddressKey(row.getAddressLine1(), row.getAddressLine2());
            String rowCity = normalizeKey(row.getCity());
            if (!wantedLine.isEmpty() && wantedLine.equals(rowLine)
                    && (wantedCity.isEmpty() || wantedCity.equals(rowCity))) {
                return row;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String normalizePincode(String value) {
        if (value == null) {
            return "";
        }
        String digits = value.replaceAll("\\D", "");
        return digits.length() == 6 ? digits : "";
    }

    private static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase()
                .replace('\u00b7', ' ')
                .replaceAll("[,|./\\\\-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String normalizeAddressKey(String line1, String line2) {
        String joined = ((line1 != null ? line1 : "") + " " + (line2 != null ? line2 : "")).trim();
        return normalizeKey(joined);
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
String phone = request.getPhone();

if (phone == null || phone.trim().isEmpty()) {
    phone = getCurrentUser().getContactNumber();
}

if (phone == null || phone.trim().isEmpty()) {
    throw new IllegalArgumentException("Phone number is required");
}

address.setPhone(phone);
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