package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqResponse {
    private Integer id;
    private Integer categoryId;
    private String question;
    private String answer;
    private Integer sortOrder;
    private Boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
