package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryResponse {
    private Integer id;
    private String countryName;
    private String countryCode;
    private Boolean status;
    private LocalDateTime createdAt;
}
