package com.ecommerce.authdemo.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private Integer id;
    private Integer orderId;
    private String invoiceNumber;
    private String invoicePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
