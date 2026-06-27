package com.ecommerce.authdemo.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {

    private String name;

    private String username;

    private String email;

    private String contactNumber;

    private LocalDate dateOfBirth;

    private String gender;
}
