package com.ecommerce.authdemo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemDTO {

    private Long orderItemId;
    private Long productId;
    private Long variantId;
    private String productName;

    private String productImage;

    private Integer quantity;
    private Double price;
    private Double mrpPrice;
    private Double total;

    private String sku;
    private String hsnCode;

    private String color;
    private String size;

    private Double weight;

    private Double lengthCm;
    private Double widthCm;
    private Double heightCm;

    private Double packageDeadWeight;
    private Double volumetricWeight;
    private Double chargeableWeight;

    private String sellerName;
    private Long sellerId;

    /** True when products.is_customized_product = 1 for this line. */
    private Boolean isCustomizable;

    /** Parsed from products.custom_required_fields. */
    private java.util.List<CustomRequiredFieldDTO> customRequiredFields;

    /** Saved rows from order_item_custom_details. */
    private java.util.List<OrderItemCustomDetailDTO> customDetails;

    private Boolean customDetailsComplete;

}