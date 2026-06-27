package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PincodeResponse {
    private Integer id;
    private Integer countryId;
    private Integer stateId;
    private Integer cityId;
    private Integer areaId;
    private String pincode;
    private Boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
