package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReviewRequestDTO {
    @NotNull(message = "productId is required")
    private Long productId;

    private Long userId;

    @NotBlank(message = "name is required")
    private String name;

    @Email(message = "email must be valid")
    private String email;

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be at least 1")
    @Max(value = 5, message = "rating must be at most 5")
    private Integer rating;

    @NotBlank(message = "comment is required")
    private String comment;

    private String imagePath;

    private Boolean status;
}
