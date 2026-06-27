package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_location")
@Data
public class UserLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private Double latitude;

    private Double longitude;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}