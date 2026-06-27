package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateResponse {
    private Integer id;
    private String stateName;
    private Integer countryId;
    private Boolean status;
    private LocalDateTime createdAt;
}
