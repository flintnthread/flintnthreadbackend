package com.ecommerce.authdemo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {

    private Long id;

    private String name;

    private String username;

    private String email;

    private String contactNumber;

    private String role;

    private Boolean verified;

    private String status;
}
