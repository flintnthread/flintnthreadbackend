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
@Table(name = "homepage_banners")
@Getter
@Setter
public class HomepageBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String section;

    @Column(nullable = false)
    private Integer position = 1;

    @Column(nullable = false)
    private String title;

    @Column(name = "alt_text", nullable = false)
    private String altText;

    @Column(name = "image_path", nullable = false, length = 500)
    private String imagePath;

    @Column(name = "link_url", length = 500)
    private String linkUrl = "shop-grid.php";

    @Column(name = "button_text", length = 100)
    private String buttonText;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

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
        if (position == null) {
            position = 1;
        }
        if (isActive == null) {
            isActive = true;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
        if (linkUrl == null || linkUrl.isBlank()) {
            linkUrl = "shop-grid.php";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
