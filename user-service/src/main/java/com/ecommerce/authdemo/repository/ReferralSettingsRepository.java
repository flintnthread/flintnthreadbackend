package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ReferralSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

    public interface ReferralSettingsRepository extends JpaRepository<ReferralSettings,Integer> {

        Optional<ReferralSettings> findBySettingKey(String key);
    }

