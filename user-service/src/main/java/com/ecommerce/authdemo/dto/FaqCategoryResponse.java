package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqCategoryResponse {
    private Integer id;
    private String categoryName;
    private String categoryIcon;
    private Integer sortOrder;
    private Boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
