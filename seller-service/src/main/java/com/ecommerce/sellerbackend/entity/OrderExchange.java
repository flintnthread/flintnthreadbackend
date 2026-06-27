package com.ecommerce.sellerbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_exchanges")
@Getter
@Setter
public class OrderExchange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_item_id")
    private Integer orderItemId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "variant_id")
    private Integer variantId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "exchange_color")
    private Integer exchangeColor;

    @Column(name = "exchange_size")
    private Integer exchangeSize;

    @Column(length = 50)
    private String status;

    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    @Column(name = "processed_by")
    private Integer processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "shiprocket_order_id", length = 100)
    private String shiprocketOrderId;

    @Column(name = "shiprocket_shipment_id", length = 100)
    private String shiprocketShipmentId;

    @Column(name = "shiprocket_awb_code", length = 100)
    private String shiprocketAwbCode;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "shipping_provider", length = 100)
    private String shippingProvider;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
