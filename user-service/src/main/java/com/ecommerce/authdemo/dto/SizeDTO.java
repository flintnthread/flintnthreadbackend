package com.ecommerce.authdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SizeDTO {
    private Long id;
    private String name;
    private String code;
}
