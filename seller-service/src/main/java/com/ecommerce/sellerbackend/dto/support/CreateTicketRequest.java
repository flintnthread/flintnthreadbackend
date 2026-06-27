package com.ecommerce.sellerbackend.dto.support;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTicketRequest {

    @NotNull(message = "Seller ID is required")
    private Long sellerId;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Priority is required")
    private String priority;

    private String description;
}
