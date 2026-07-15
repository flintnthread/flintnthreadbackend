package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "price", nullable = false)
    private Double price;

    /** Not present on all MySQL schemas — kept in memory for DTOs/Shiprocket enrich. */
    @Transient
    private Double mrpPrice;

    @Column(name = "total", nullable = false)
    private Double total;

    @Column(name = "status")
    private String status;

    @Column(name = "product_image_path")
    private String productImagePath;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private String productName;

    @Transient
    private String sku;

    @Transient
    private String hsnCode;

    @Transient
    private String color;

    @Transient
    private String size;

    @Transient
    private Double weight;

    @Transient
    private Double lengthCm;

    @Transient
    private Double widthCm;

    @Transient
    private Double heightCm;

    @Transient
    private String sellerName;

    @Transient
    private Double packageDeadWeight;

    @Transient
    private Double volumetricWeight;

    @Transient
    private Double chargeableWeight;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "processing";
        }
    }
}
