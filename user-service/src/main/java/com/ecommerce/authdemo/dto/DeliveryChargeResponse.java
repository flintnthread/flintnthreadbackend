package com.ecommerce.authdemo.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryChargeResponse {
    private Integer id;
    private String weightSlab;
    private BigDecimal weightMin;
    private BigDecimal weightMax;
    private BigDecimal intraCityCharge;
    private BigDecimal metroMetroCharge;
    private Boolean isCustom;
    private Boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
