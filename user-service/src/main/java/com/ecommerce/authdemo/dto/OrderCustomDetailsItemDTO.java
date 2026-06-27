package com.ecommerce.authdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCustomDetailsItemDTO {

    private Long orderItemId;
    private Long productId;
    private Long variantId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private boolean customizable;
    private List<CustomRequiredFieldDTO> requiredFields;
    private List<OrderItemCustomDetailDTO> savedDetails;
    private boolean detailsComplete;
}
