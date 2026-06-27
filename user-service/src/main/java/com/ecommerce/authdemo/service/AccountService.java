package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.UpdateProfileRequest;
import com.ecommerce.authdemo.dto.UserProfileDTO;
import com.ecommerce.authdemo.entity.User;

public interface AccountService {

    User getProfile(Long requestedUserId);

    UserProfileDTO getProfileDto(Long requestedUserId);

    User updateProfile(Long requestedUserId, String username, String email);

    UserProfileDTO updateProfileDto(Long requestedUserId, UpdateProfileRequest request);

    void deleteAccount(Long requestedUserId);

    User updateContactNumber(Long requestedUserId, String contactNumber);

    void deactivateAccount(Long requestedUserId);
}