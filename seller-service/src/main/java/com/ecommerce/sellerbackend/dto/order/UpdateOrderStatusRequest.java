package com.ecommerce.sellerbackend.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrderStatusRequest {

    @NotBlank(message = "Status is required.")
    private String status;

    /** Optional note stored in order_status_history.comment */
    private String comment;
}
