package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sizes")
@Getter
@Setter
public class Size {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "size_name", nullable = false, length = 100)
    private String name;

    @Column(name = "size_code", nullable = false, length = 20)
    private String code;

    /** DB default 1 when null on insert. */
    @Column(name = "status")
    private Integer status;

    @Column(name = "seller_id")
    private Integer sellerId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Size() {}

    public Size(String name, String code) {
        this.name = name;
        this.code = code;
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
