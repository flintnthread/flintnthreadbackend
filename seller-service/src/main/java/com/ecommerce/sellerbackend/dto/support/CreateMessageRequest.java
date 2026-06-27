package com.ecommerce.sellerbackend.dto.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMessageRequest {

    @NotNull(message = "Ticket ID is required")
    private Long ticketId;

    @NotBlank(message = "Sender type is required")
    private String senderType;

    private Long senderId;

    private String message;

    private String attachment;
}
