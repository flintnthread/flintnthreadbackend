package com.ecommerce.sellerbackend.dto;

import com.ecommerce.sellerbackend.entity.CategoryRequest;
import com.ecommerce.sellerbackend.entity.CategoryRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Getter
@Builder
public class CategoryRequestResponse {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private Long id;
    private String categoryName;
    private String description;
    private String reason;
    private String status;
    private String submittedAt;

    public static CategoryRequestResponse from(CategoryRequest request) {
        return CategoryRequestResponse.builder()
                .id(request.getId())
                .categoryName(request.getCategoryName())
                .description(request.getDescription() != null ? request.getDescription() : "")
                .reason(request.getReason() != null ? request.getReason() : "")
                .status(toDisplayStatus(request.getStatus()))
                .submittedAt(
                        request.getCreatedAt() != null
                                ? request.getCreatedAt().format(DISPLAY_DATE)
                                : "")
                .build();
    }

    private static String toDisplayStatus(CategoryRequestStatus status) {
        if (status == null) {
            return "Pending";
        }
        return switch (status) {
            case approved -> "Approved";
            case rejected -> "Rejected";
            default -> "Pending";
        };
    }
}
