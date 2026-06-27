package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ReferralSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReferralSettingRepository extends JpaRepository<ReferralSetting,Integer> {

    Optional<ReferralSetting> findBySettingKey(String key);
}