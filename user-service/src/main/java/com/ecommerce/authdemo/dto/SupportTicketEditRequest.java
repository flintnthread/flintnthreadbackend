package com.ecommerce.authdemo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupportTicketEditRequest {

    @Size(max = 50, message = "Type must not exceed 50 characters")
    private String type;

    @Size(max = 5000, message = "Message must not exceed 5000 characters")
    @JsonAlias("description")
    private String message;
}
