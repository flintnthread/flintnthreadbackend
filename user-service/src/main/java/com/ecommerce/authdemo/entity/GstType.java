package com.ecommerce.authdemo.entity;

import jakarta.persistence.*;
import lombok.*;

    @Entity
    @Table(name = "gst_types")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class GstType {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        @Column(name = "type_code", nullable = false, length = 10)
        private String typeCode;

        @Column(name = "type_name", nullable = false, length = 100)
        private String typeName;

        @Column(name = "description", columnDefinition = "TEXT")
        private String description;
    }

