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
@Table(name = "order_item_cancellations")
@Getter
@Setter
public class OrderItemCancellation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_item_id")
    private Integer orderItemId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(length = 50)
    private String status;

    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    @Column(name = "processed_by")
    private Integer processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
