package com.ecommerce.sellerbackend.dto.support;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqResponse {
    private Integer id;
    private Integer categoryId;
    private String categoryName;
    private String question;
    private String answer;
    private Integer sortOrder;
    private Boolean isSeller;
}
