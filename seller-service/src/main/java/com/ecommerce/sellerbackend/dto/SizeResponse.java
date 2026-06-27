package com.ecommerce.sellerbackend.dto;

import com.ecommerce.sellerbackend.entity.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Getter
@Builder
public class SizeResponse {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private Long id;
    private String name;
    private String code;
    private String status;
    private String createdAt;
  /** True when the logged-in seller owns this row (can edit/delete). */
    private boolean owned;

    public static SizeResponse from(Size size, Long sellerId) {
        boolean owned = size.getSellerId() != null && size.getSellerId().equals(sellerId);
        return SizeResponse.builder()
                .id(size.getId())
                .name(size.getSizeName())
                .code(size.getSizeCode())
                .status(Boolean.TRUE.equals(size.getStatus()) ? "Active" : "Inactive")
                .createdAt(size.getCreatedAt() != null ? size.getCreatedAt().format(DISPLAY_DATE) : "")
                .owned(owned)
                .build();
    }
}
