package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shiprocket_sync_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiprocketSyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @Column(name = "order_number", length = 50)
    private String orderNumber;

    @Column(name = "shiprocket_order_id", length = 100)
    private String shiprocketOrderId;

    @Column(name = "action", length = 50)
    private String action;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
