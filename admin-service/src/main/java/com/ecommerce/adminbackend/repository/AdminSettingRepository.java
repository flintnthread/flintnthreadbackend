package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.AdminSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminSettingRepository extends JpaRepository<AdminSetting, Long> {

    Optional<AdminSetting> findBySettingKey(String settingKey);
}
