package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

    @Entity
    @Table(name="referral_settings")
    @Getter
    @Setter
    public class ReferralSettings {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        @Column(unique = true)
        private String settingKey;

        @Column(columnDefinition = "TEXT")
        private String settingValue;

        @Column(columnDefinition = "TEXT")
        private String description;
    }

