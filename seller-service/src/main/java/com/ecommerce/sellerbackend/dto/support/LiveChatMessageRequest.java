package com.ecommerce.sellerbackend.dto.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveChatMessageRequest {

    @NotNull
    private Integer sellerId;

    @NotBlank
    private String message;
}
