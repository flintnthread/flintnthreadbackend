package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeImageResponse {
    private Integer id;
    private Integer exchangeId;
    private String imagePath;
    private LocalDateTime createdAt;
}
