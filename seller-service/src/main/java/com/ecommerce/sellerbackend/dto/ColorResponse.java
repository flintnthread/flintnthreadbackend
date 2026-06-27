package com.ecommerce.sellerbackend.dto;

import com.ecommerce.sellerbackend.entity.Color;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Getter
@Builder
public class ColorResponse {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private Long id;
    private String name;
    private String hex;
    private String status;
    private String createdAt;
    private boolean owned;

    public static ColorResponse from(Color color, Long sellerId) {
        boolean owned = color.getSellerId() != null && color.getSellerId().equals(sellerId);
        return ColorResponse.builder()
                .id(color.getId())
                .name(color.getColorName())
                .hex(color.getColorCode())
                .status(Boolean.TRUE.equals(color.getStatus()) ? "Active" : "Inactive")
                .createdAt(color.getCreatedAt() != null ? color.getCreatedAt().format(DISPLAY_DATE) : "")
                .owned(owned)
                .build();
    }
}
