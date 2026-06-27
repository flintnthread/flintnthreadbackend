package com.ecommerce.authdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemCustomDetailDTO {

    private String fieldKey;
    private String fieldLabel;
    private String valueText;
    private String valueFile;
}
