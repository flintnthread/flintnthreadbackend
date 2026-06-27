package com.ecommerce.authdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomRequiredFieldDTO {

    /** Stable key stored in order_item_custom_details.field_key (e.g. f0, f1). */
    private String key;

    private String label;

    /** text | image */
    private String type;

    private boolean required;
}
