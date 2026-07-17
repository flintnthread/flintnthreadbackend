package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.ApiResponse;
import com.ecommerce.authdemo.dto.ProfileResponseDTO;
import com.ecommerce.authdemo.dto.ShopperDTO;
import com.ecommerce.authdemo.dto.UpdateProfileDTO;
import com.ecommerce.authdemo.entity.Shopper;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.repository.ShopperRepository;
import com.ecommerce.authdemo.repository.UserRepository;
import com.ecommerce.authdemo.service.ImageUploadService;
import com.ecommerce.authdemo.service.UserAccountDeletionService;
import com.ecommerce.authdemo.service.UserService;
import com.ecommerce.authdemo.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
    @RequiredArgsConstructor
    public class UserServiceImpl implements UserService {

        private final UserRepository userRepository;
        private final ShopperRepository shopperRepository;
        private final SecurityUtil securityUtil;
        private final ImageUploadService imageUploadService;
        private final UserAccountDeletionService userAccountDeletionService;


    @Override
        public ProfileResponseDTO getProfile() {

            User user = securityUtil.getCurrentUser();

            Shopper active = shopperRepository
                    .findByUserIdAndIsActiveTrue(user.getId())
                    .orElse(null);

            ShopperDTO shopperDTO = active != null
                    ? new ShopperDTO(active.getId(), active.getName(), true)
                    : null;

            return ProfileResponseDTO.builder()
                    .id(user.getId())
                    .name(user.getUsername())
                    .email(toPublicEmail(user.getEmail()))
                    .contactNumber(user.getContactNumber())
                    .profileImage(user.getProfileImage())
                    .activeShopper(shopperDTO)
                    .build();
        }

        private static String toPublicEmail(String email) {
            if (email == null || email.isBlank()) {
                return null;
            }
            String trimmed = email.trim();
            String lower = trimmed.toLowerCase();
            if (lower.endsWith("@mobile.flintnthread.in")
                    || lower.endsWith("@mobile.flintnthread.online")
                    || lower.matches("^\\d{10}@.*")) {
                return null;
            }
            return trimmed;
        }

        @Override
        public ProfileResponseDTO updateProfile(UpdateProfileDTO dto) {

            User user = securityUtil.getCurrentUser();

            if (dto.getName() != null) {
                user.setUsername(dto.getName());
            }

            if (dto.getContactNumber() != null) {
                user.setContactNumber(dto.getContactNumber());
            }

            if (dto.getProfileImage() != null && !dto.getProfileImage().trim().isEmpty()) {
                user.setProfileImage(dto.getProfileImage().trim());
            }

            userRepository.save(user);

            return getProfile();
        }

    @Override
    public ApiResponse<?> uploadProfileImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Image file is required");
        }

        try {
            String imageUrl = imageUploadService.uploadImage(file, "profile");

            User user = securityUtil.getCurrentUser();

            user.setProfileImage(imageUrl);
            userRepository.save(user);

            return new ApiResponse<>(true, "Profile image updated successfully", imageUrl);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
        }
    }
    @Override
    public User getCurrentUser() {

        Long userId =
                securityUtil
                        .getCurrentUserId();

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        ));
    }

        @Override
        @Transactional
        public void deleteAccount() {
            User user = securityUtil.getCurrentUser();
            userAccountDeletionService.deleteUserAccount(user.getId());
        }
    }

