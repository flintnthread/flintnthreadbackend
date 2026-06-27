package com.ecommerce.authdemo.dto;

import lombok.Data;

@Data
public class LocationRequest {

    private Long userId;
    private Double latitude;
    private Double longitude;

}