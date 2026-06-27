package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "banners")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String name;

    @Column(name = "image", nullable = false, length = 255)
    private String desktopImage;

    @Column(name = "mobile_image", length = 255)
    private String mobileImage;

    @Column(name = "text_content", columnDefinition = "TEXT")
    private String description;

    @Column(name = "button_url", length = 255)
    private String targetUrl;

    @Column(name = "button_text", length = 100)
    private String buttonText;

    @Column(name = "text_position", length = 20)
    private String textAlign;

    @Column(name = "size", length = 20)
    private String bannerType;

    @Column(name = "status")
    private Integer status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
