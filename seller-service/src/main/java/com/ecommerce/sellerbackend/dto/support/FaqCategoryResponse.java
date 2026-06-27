package com.ecommerce.sellerbackend.dto.support;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqCategoryResponse {
    private Integer id;
    private String categoryName;
    private String categoryIcon;
    private Integer sortOrder;
    private List<FaqResponse> faqs;
}
