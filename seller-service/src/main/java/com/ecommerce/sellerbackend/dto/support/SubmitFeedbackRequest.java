package com.ecommerce.sellerbackend.dto.support;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitFeedbackRequest {

    @NotNull(message = "sellerId is required")
    private Long sellerId;

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5")
    private Integer rating;

    // Feedback text is optional (UI allows submit without typing).
    private String feedbackText;
}

