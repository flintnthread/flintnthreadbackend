package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.Enum.UserStatus;
import com.ecommerce.authdemo.dto.UpdateProfileRequest;
import com.ecommerce.authdemo.dto.UserProfileDTO;
import com.ecommerce.authdemo.entity.Address;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.repository.AddressRepository;
import com.ecommerce.authdemo.repository.UserRepository;
import com.ecommerce.authdemo.service.AccountService;
import com.ecommerce.authdemo.service.UserAccountDeletionService;
import com.ecommerce.authdemo.util.SecurityUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final SecurityUtil securityUtil;
    private final UserAccountDeletionService userAccountDeletionService;

    private Long resolveCurrentUserId(Long requestedUserId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        if (requestedUserId != null && !requestedUserId.equals(currentUserId)) {
            throw new RuntimeException("Access denied");
        }
        return currentUserId;
    }

    @Override
    public User getProfile(Long requestedUserId) {
        Long userId = resolveCurrentUserId(requestedUserId);

        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    @Override
    public UserProfileDTO getProfileDto(Long requestedUserId) {
        Long userId = resolveCurrentUserId(requestedUserId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return toProfileDto(user);
    }

    @Override
    @Transactional
    public UserProfileDTO updateProfileDto(Long requestedUserId, UpdateProfileRequest request) {
        Long userId = resolveCurrentUserId(requestedUserId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        String username = firstNonBlank(
                request != null ? request.getName() : null,
                request != null ? request.getUsername() : null
        );
        if (username != null) {
            if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
                throw new RuntimeException("Username already taken");
            }
            user.setUsername(username);
        }

        if (request != null && request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String email = request.getEmail().trim();
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(email);
        }

        if (request != null && request.getContactNumber() != null) {
            user.setContactNumber(request.getContactNumber().trim());
        }

        if (request != null && request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        if (request != null && request.getGender() != null && !request.getGender().trim().isEmpty()) {
            user.setGender(request.getGender().trim().toLowerCase());
        }

        return toProfileDto(userRepository.save(user));
    }

    private UserProfileDTO toProfileDto(User user) {
        String location = resolveCurrentLocation(user.getId());
        return UserProfileDTO.builder()
                .id(user.getId())
                .fullName(user.getUsername())
                .username(user.getUsername())
                .email(user.getEmail())
                .contactNumber(user.getContactNumber())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .profileImage(user.getProfileImage())
                .currentLocation(location)
                .verified(user.isVerified())
                .build();
    }

    private String resolveCurrentLocation(Long userId) {
        Address address = addressRepository.findByUserIdAndIsDefaultTrue(userId).orElse(null);
        if (address == null) {
            List<Address> rows = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
            address = rows.isEmpty() ? null : rows.get(0);
        }
        if (address == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        if (address.getCity() != null && !address.getCity().isBlank()) {
            parts.add(address.getCity().trim());
        }
        if (address.getState() != null && !address.getState().isBlank()) {
            parts.add(address.getState().trim());
        }
        if (address.getCountry() != null && !address.getCountry().isBlank()) {
            parts.add(address.getCountry().trim());
        } else if (parts.size() > 0) {
            parts.add("India");
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return null;
    }

    @Override
    @Transactional
    public User updateProfile(Long requestedUserId, String username, String email) {
        Long userId = resolveCurrentUserId(requestedUserId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Update username
        if (username != null && !username.trim().isEmpty()) {

            if (!username.equals(user.getUsername()) &&
                    userRepository.existsByUsername(username)) {

                throw new RuntimeException("Username already taken");
            }

            user.setUsername(username);
        }

        // Update email
        if (email != null && !email.trim().isEmpty()) {

            if (!email.equals(user.getEmail()) &&
                    userRepository.existsByEmail(email)) {

                throw new RuntimeException("Email already exists");
            }

            user.setEmail(email);
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteAccount(Long requestedUserId) {
        Long userId = resolveCurrentUserId(requestedUserId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        userAccountDeletionService.deleteUserAccount(userId);
    }

    @Override
    public User updateContactNumber(Long requestedUserId, String contactNumber) {
        Long userId = resolveCurrentUserId(requestedUserId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setContactNumber(contactNumber);

        return userRepository.save(user);
    }



    @Override
    public void deactivateAccount(Long requestedUserId) {
        Long userId = resolveCurrentUserId(requestedUserId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(UserStatus.valueOf("inactive"));

        userRepository.save(user);
    }
}