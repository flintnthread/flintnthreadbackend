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
@Table(name = "order_item_custom_details")
@Getter
@Setter
public class OrderItemCustomDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_item_id")
    private Integer orderItemId;

    @Column(name = "field_key", length = 100)
    private String fieldKey;

    @Column(name = "field_label", length = 255)
    private String fieldLabel;

    @Column(name = "value_text", columnDefinition = "TEXT")
    private String valueText;

    @Column(name = "value_file", length = 500)
    private String valueFile;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
