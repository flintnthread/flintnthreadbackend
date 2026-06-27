package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermsConditionsResponse {
    private Integer id;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
