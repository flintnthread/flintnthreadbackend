package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.UpdateProfileRequest;
import com.ecommerce.authdemo.dto.UserProfileDTO;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.service.AccountService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /*
     ---------------------------------------
     GET USER PROFILE
     ---------------------------------------
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getMyProfile() {
        return ResponseEntity.ok(accountService.getProfileDto(null));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileDTO> getProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(accountService.getProfileDto(userId));
    }


    /*
     ---------------------------------------
     UPDATE USER PROFILE
     ---------------------------------------
     */
    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(accountService.updateProfileDto(null, request));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileDTO> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(accountService.updateProfileDto(userId, request));
    }


    /*
     ---------------------------------------
     UPDATE CONTACT NUMBER
     ---------------------------------------
     */
    @PutMapping("/me/contact")
    public ResponseEntity<User> updateMyContactNumber(
            @RequestParam String contactNumber) {

        User updatedUser =
                accountService.updateContactNumber(null, contactNumber);

        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{userId}/contact")
    public ResponseEntity<User> updateContactNumber(
            @PathVariable Long userId,
            @RequestParam String contactNumber) {

        User updatedUser =
                accountService.updateContactNumber(userId, contactNumber);

        return ResponseEntity.ok(updatedUser);
    }


    /*
     ---------------------------------------
     DEACTIVATE ACCOUNT
     ---------------------------------------
     */
    @PutMapping("/me/deactivate")
    public ResponseEntity<String> deactivateMyAccount() {

        accountService.deactivateAccount(null);

        return ResponseEntity.ok("Account deactivated successfully");
    }

    @PutMapping("/{userId}/deactivate")
    public ResponseEntity<String> deactivateAccount(
            @PathVariable Long userId) {

        accountService.deactivateAccount(userId);

        return ResponseEntity.ok("Account deactivated successfully");
    }


    /*
     ---------------------------------------
     DELETE ACCOUNT
     ---------------------------------------
     */
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteMyAccount() {

        accountService.deleteAccount(null);

        return ResponseEntity.ok("Account deleted successfully");
    }

    /** Fallback when DELETE is blocked (proxy / browser). */
    @PostMapping("/me/delete")
    public ResponseEntity<String> deleteMyAccountPost() {
        return deleteMyAccount();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteAccount(
            @PathVariable Long userId) {

        accountService.deleteAccount(userId);

        return ResponseEntity.ok("Account deleted successfully");
    }

}