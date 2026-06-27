package com.ecommerce.authdemo.entity;

import com.ecommerce.authdemo.dto.Enum.DeliveryType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "delivery_type")
    private DeliveryType deliveryType = DeliveryType.metro_metro;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", precision = 12, scale = 2)
    private BigDecimal finalAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "currency")
    private String currency = "USD";

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "shipping_amount", precision = 10, scale = 2)
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}