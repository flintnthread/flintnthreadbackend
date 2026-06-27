package com.ecommerce.sellerbackend.dto.order;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderGstDto {
    private Integer id;
    private Long orderId;
    private String gstNumber;
    private String gstInfo;
    private String createdAt;
    private String updatedAt;
}
