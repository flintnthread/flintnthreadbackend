package com.ecommerce.adminbackend.entity.cms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "site_settings")
@Getter
@Setter
public class SiteSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "setting_key", nullable = false, length = 100, unique = true)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, length = 500)
    private String settingValue = "";

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
