package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.UpdateProfileDTO;
import com.ecommerce.authdemo.service.UserService;
import com.ecommerce.authdemo.dto.PushTokenRequestDTO;
import com.ecommerce.authdemo.entity.User;
import com.ecommerce.authdemo.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
    @RequestMapping("/api/user")
    @RequiredArgsConstructor
    public class UserController {

        private final UserService userService;
    private final UserRepository userRepository;


    @GetMapping("/profile")
        public ResponseEntity<?> getProfile() {
            return ResponseEntity.ok(userService.getProfile());
        }

        @PutMapping(value = "/profile", consumes = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
            return ResponseEntity.ok(userService.updateProfile(dto));
        }

    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfileFormData(
            @RequestParam("name") String name,
            @RequestParam(value = "contactNumber", required = false) String contactNumber,
            @RequestParam(value = "profileImage", required = false) String profileImage,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setName(name);
        dto.setContactNumber(contactNumber);
        dto.setProfileImage(profileImage);

        userService.updateProfile(dto);
        if (file != null && !file.isEmpty()) {
            userService.uploadProfileImage(file);
        }
        return ResponseEntity.ok(userService.getProfile());
    }

    @PostMapping("/profile/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.uploadProfileImage(file));

    }

    @PostMapping("/push-token")
    public ResponseEntity<?> savePushToken(
            @RequestBody
            PushTokenRequestDTO dto
    ) {

        User user =
                userService.getCurrentUser();

        user.setExpoPushToken(
                dto.getExpoPushToken()
        );

        userRepository.save(user);

        return ResponseEntity.ok(
                "Push token saved"
        );
    }
    
        @DeleteMapping
        public ResponseEntity<?> deleteAccount() {
            userService.deleteAccount();
            return ResponseEntity.ok("Account deleted successfully");
        }
    }

