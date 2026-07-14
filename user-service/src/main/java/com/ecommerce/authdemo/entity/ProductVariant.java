package com.ecommerce.authdemo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonIgnore
    private Product product;

    private String color;
    private String size;
    private String sku;

    private BigDecimal basePrice;
    private BigDecimal mrpExclGst;
    private BigDecimal mrpPrice;
    private BigDecimal sellingPrice;
    private BigDecimal finalPrice;

    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;

    private BigDecimal taxPercentage;
    private BigDecimal taxAmount;
    private BigDecimal mrpInclGst;

    private BigDecimal intraCityDeliveryCharge;
    private BigDecimal metroMetroDeliveryCharge;
    private BigDecimal totalPriceIntraCity;
    private BigDecimal totalPriceMetroMetro;

    private BigDecimal commissionPercentage;
    private BigDecimal commissionAmount;

    private Integer stock;
    private String videoPath;
    private BigDecimal weight;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Unit selling for cart / UI price:
     * sellingPrice → finalPrice → basePrice
     */
    public BigDecimal resolveSellingUnitPrice() {
        return firstPositive(sellingPrice, finalPrice, basePrice);
    }

    /**
     * Unit MRP for UI original / strike price.
     * Admin form stores MRP excl. GST in {@code mrp_excl_gst} — prefer that.
     * DB {@code mrp_price} is often selling+GST and is only used when clearly higher than sell.
     */
    public BigDecimal resolveMrpUnitPrice() {
        BigDecimal excl = firstPositive(mrpExclGst);
        if (excl != null) {
            return excl.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        BigDecimal incl = firstPositive(mrpInclGst);
        if (incl != null) {
            return incl;
        }
        BigDecimal legacy = firstPositive(mrpPrice);
        if (legacy == null) {
            return null;
        }
        BigDecimal sell = firstPositive(finalPrice, sellingPrice, basePrice);
        if (sell != null && legacy.compareTo(sell) > 0) {
            return legacy;
        }
        return null;
    }

    private static BigDecimal firstPositive(BigDecimal... candidates) {
        if (candidates == null) {
            return null;
        }
        for (BigDecimal b : candidates) {
            if (b != null && b.compareTo(BigDecimal.ZERO) > 0) {
                return b;
            }
        }
        return null;
    }
}