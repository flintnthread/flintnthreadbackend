package com.ecommerce.adminbackend.entity.cms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "banners")
@Getter
@Setter
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String image;

    @Column(name = "mobile_image")
    private String mobileImage;

    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "button_url")
    private String buttonUrl;

    @Column(name = "button_text", length = 100)
    private String buttonText = "Shop Now";

    @Column(name = "text_position", length = 20)
    private String textPosition = "left";

    @Column(length = 20)
    private String size = "full";

    /** DB tinyint: 1 = active, 0 = inactive */
    @Column
    private Integer status = 1;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (buttonText == null || buttonText.isBlank()) {
            buttonText = "Shop Now";
        }
        if (textPosition == null || textPosition.isBlank()) {
            textPosition = "left";
        }
        if (size == null || size.isBlank()) {
            size = "full";
        }
        if (status == null) {
            status = 1;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
