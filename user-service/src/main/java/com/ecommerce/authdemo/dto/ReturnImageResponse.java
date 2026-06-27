package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnImageResponse {

    private Long id;

    private Long returnId;

    private String imagePath;

    private LocalDateTime createdAt;
}