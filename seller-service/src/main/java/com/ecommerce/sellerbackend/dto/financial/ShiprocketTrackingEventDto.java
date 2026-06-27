package com.ecommerce.sellerbackend.dto.financial;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShiprocketTrackingEventDto {
    private String date;
    private String time;
    private String status;
    private String location;
    private String description;
    private String type;
}
