package com.ecommerce.authdemo.entity;

import com.ecommerce.authdemo.dto.Enum.AdminStatus;
import com.ecommerce.authdemo.converter.AdminStatusConverter;

import jakarta.persistence.*;
import lombok.Data;

    @Data
    @Entity
    @Table(name = "admin_users")
    public class AdminUser {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String name;

        private String email;

        private String password;

        @Convert(converter = AdminStatusConverter.class)
        @Column(name = "status")
        private AdminStatus status;

    }

