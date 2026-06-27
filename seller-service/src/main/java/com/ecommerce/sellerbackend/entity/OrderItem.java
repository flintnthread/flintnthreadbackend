package com.ecommerce.sellerbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(name = "product_name")
    private String productName;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "hsn_code", length = 50)
    private String hsnCode;

    @Column(name = "color", length = 100)
    private String color;

    @Column(name = "size", length = 100)
    private String size;

    @Column(name = "weight", precision = 10, scale = 2)
    private BigDecimal weight;

    @Column(name = "length_cm", precision = 10, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "width_cm", precision = 10, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "height_cm", precision = 10, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "seller_name")
    private String sellerName;

    @Column(name = "package_dead_weight", precision = 10, scale = 2)
    private BigDecimal packageDeadWeight;

    @Column(name = "volumetric_weight", precision = 10, scale = 2)
    private BigDecimal volumetricWeight;

    @Column(name = "chargeable_weight", precision = 10, scale = 2)
    private BigDecimal chargeableWeight;
}
