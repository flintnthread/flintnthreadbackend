package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "colors")
@Getter
@Setter
public class Color {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "color_name", nullable = false, length = 100)
    private String name;

    @Column(name = "color_code", nullable = false, length = 20)
    private String code;

    /** Not stored on {@code colors} in this schema; keep for API/constructors only. */
    @Transient
    private String hex;

    @Column(name = "status")
    private Integer status;

    @Column(name = "seller_id")
    private Integer sellerId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Color() {}

    public Color(String name, String code, String hex) {
        this.name = name;
        this.code = code;
        this.hex = hex;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = 1;
        }
    }
}
