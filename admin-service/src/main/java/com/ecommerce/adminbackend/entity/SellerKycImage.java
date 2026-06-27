package com.ecommerce.adminbackend.entity;

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
@Table(name = "seller_kyc_images")
@Getter
@Setter
public class SellerKycImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "image_path", nullable = false)
    private String imagePath;

    @Column(name = "image_type", nullable = false)
    private String imageType = "regular";

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    @Column(name = "doc_type", nullable = false)
    private String docType = "regular";

    @Column(name = "file_name", nullable = false)
    private String fileName = "";
}
