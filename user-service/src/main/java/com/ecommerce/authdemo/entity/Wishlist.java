package com.ecommerce.authdemo.entity;



import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
     @Data
    @Entity
    @Table(name = "user_wishlist")
    public class Wishlist {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

         @ManyToOne(fetch = FetchType.LAZY)
         @JoinColumn(name = "user_id")
         private User user;

         @ManyToOne(fetch = FetchType.LAZY)
         @JoinColumn(name = "product_id")
         private Product product;

         @Column(name = "variant_id")
         private Long variantId;

         @Column(name = "created_at")
        private LocalDateTime createdAt;

        @PrePersist
        public void setCreatedAt(){
            this.createdAt = LocalDateTime.now();
        }



         // getters and setters
    }

