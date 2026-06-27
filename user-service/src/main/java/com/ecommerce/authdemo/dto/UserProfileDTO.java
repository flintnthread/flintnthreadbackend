package com.ecommerce.authdemo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UserProfileDTO {
    private Long id;
    private String fullName;
    private String username;
    private String email;
    private String contactNumber;
    private LocalDate dateOfBirth;
    private String gender;
    private String profileImage;
    private String currentLocation;
    private boolean verified;
}
