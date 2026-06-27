package com.ecommerce.authdemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PushNotificationRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    @Size(max = 50, message = "Type must not exceed 50 characters")
    private String type;

    @Size(max = 500, message = "Link must not exceed 500 characters")
    private String link;
}
