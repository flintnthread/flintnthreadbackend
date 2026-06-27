package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shiprocket_webhooks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiprocketWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "shiprocket_order_id", length = 100)
    private String shiprocketOrderId;

    @Column(name = "shipment_id", length = 100)
    private String shipmentId;

    @Column(name = "awb_code", length = 100)
    private String awbCode;

    @Column(name = "courier_name", length = 100)
    private String courierName;

    @Column(name = "current_status", length = 50)
    private String currentStatus;

    @Column(name = "webhook_data", columnDefinition = "TEXT")
    private String webhookData;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
