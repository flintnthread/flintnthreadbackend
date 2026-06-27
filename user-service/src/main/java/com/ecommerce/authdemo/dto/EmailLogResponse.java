package com.ecommerce.authdemo.dto;

import com.ecommerce.authdemo.entity.EmailLogStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLogResponse {
    private Integer id;
    private Integer userId;
    private String emailType;
    private String recipient;
    private String subject;
    private EmailLogStatus status;
    private String errorMessage;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
