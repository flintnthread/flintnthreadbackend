package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.BannerSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BannerSettingRepository extends JpaRepository<BannerSetting, Long> {

    Optional<BannerSetting> findBySettingKey(String settingKey);
}
