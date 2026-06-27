package com.ecommerce.authdemo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupportTicketRequest {
    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject must not exceed 255 characters")
    private String subject;

    @NotBlank(message = "Type is required")
    @Size(max = 50, message = "Type must not exceed 50 characters")
    private String type;

    @NotBlank(message = "Message is required")
    @JsonAlias("description")
    private String message;

    @Size(max = 255, message = "Attachment path must not exceed 255 characters")
    private String attachmentPath;
}
