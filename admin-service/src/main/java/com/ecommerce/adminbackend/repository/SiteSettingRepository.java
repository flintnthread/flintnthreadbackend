package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.cms.SiteSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SiteSettingRepository extends JpaRepository<SiteSetting, Integer> {

    Optional<SiteSetting> findBySettingKey(String settingKey);

    @Query("""
            SELECT s FROM SiteSetting s
            WHERE s.settingKey LIKE 'homepage_sec_%' OR s.settingKey LIKE 'homepage_show_%'
            ORDER BY s.settingKey ASC
            """)
    List<SiteSetting> findHomepageSectionSettings();
}
