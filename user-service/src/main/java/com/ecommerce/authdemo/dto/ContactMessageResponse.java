package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageResponse {
    private Integer id;
    private String name;
    private String email;
    private String phone;
    private String subject;
    private String message;
    private Boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
