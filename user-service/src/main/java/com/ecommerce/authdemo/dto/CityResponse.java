package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityResponse {
    private Integer id;
    private String cityName;
    private Integer stateId;
    private Integer countryId;
    private Boolean status;
    private LocalDateTime createdAt;
}
