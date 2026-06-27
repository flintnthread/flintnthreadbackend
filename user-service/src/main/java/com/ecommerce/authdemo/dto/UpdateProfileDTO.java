package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
    public class UpdateProfileDTO {

        @NotBlank
        private String name;

        private String contactNumber;

        /** Optional: pass uploaded image URL to save with profile update. */
        private String profileImage;
    }

