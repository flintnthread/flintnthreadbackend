package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_options")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "seller_id")
    private Integer sellerId;

    @Column(name = "option_name", nullable = false, length = 255)
    private String optionName;

    @Column(name = "min_days", nullable = false)
    private Integer minDays;

    @Column(name = "max_days", nullable = false)
    private Integer maxDays;

    @Column(name = "delivery_info", columnDefinition = "TEXT")
    private String deliveryInfo;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
