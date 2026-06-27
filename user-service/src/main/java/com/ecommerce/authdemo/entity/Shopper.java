package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
    @Table(name = "shoppers")
    @Getter
    @Setter
    public class Shopper {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        private String name;

        private Boolean isActive = false;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id")
        private User user;
    }

