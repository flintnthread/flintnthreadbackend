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

    @Column(name = "unit_price")
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


    @Column(name = "product_name")
    private String productName;

    @Column(name = "sku")
    private String sku;

    @Column(name = "hsn_code")
    private String hsnCode;

    @Column(name = "color")
    private String color;

    @Column(name = "size")
    private String size;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "length_cm")
    private Double lengthCm;

    @Column(name = "width_cm")
    private Double widthCm;

    @Column(name = "height_cm")
    private Double heightCm;

    @Column(name = "seller_name")
    private String sellerName;

    @Column(name = "package_dead_weight")
    private Double packageDeadWeight;

    @Column(name = "volumetric_weight")
    private Double volumetricWeight;

    @Column(name = "chargeable_weight")
    private Double chargeableWeight;


    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        
        if (this.status == null) {
            this.status = "processing";
        }
    }

    @PreUpdate
    public void preUpdate() {
        // Removed updatedAt reference
    }
}