package com.ecommerce.adminbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "total", precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "status")
    private String status;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "product_image_path", length = 500)
    private String productImagePath;

    @Column(name = "created_at")
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
    private BigDecimal weight;

    @Transient
    private BigDecimal lengthCm;

    @Transient
    private BigDecimal widthCm;

    @Transient
    private BigDecimal heightCm;

    @Transient
    private String sellerName;

    @Transient
    private BigDecimal packageDeadWeight;

    @Transient
    private BigDecimal volumetricWeight;

    @Transient
    private BigDecimal chargeableWeight;
}
