package com.ecommerce.sellerbackend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LocationItemResponse {
    private Integer id;
    private String name;
    private Integer parentId;
    private String parentName;
}
