package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.AdminSetting;
import com.ecommerce.adminbackend.repository.AdminSettingRepository;
import com.ecommerce.adminbackend.service.SettingsAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingsAdminServiceImpl extends BaseAdminService implements SettingsAdminService {

    private static final String KEY_B2C = "commission_b2c";
    private static final String KEY_B2B = "commission_b2b";
    private static final String DEFAULT_B2C = "15";
    private static final String DEFAULT_B2B = "7";

    private final AdminSettingRepository settingRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getCommission() {
        Map<String, String> commission = new LinkedHashMap<>();
        commission.put("b2c", readSetting(KEY_B2C, DEFAULT_B2C));
        commission.put("b2b", readSetting(KEY_B2B, DEFAULT_B2B));
        return commission;
    }

    @Override
    @Transactional
    public Map<String, String> updateCommission(String b2c, String b2b) {
        if (b2c != null && !b2c.isBlank()) {
            upsert(KEY_B2C, b2c.trim());
        }
        if (b2b != null && !b2b.isBlank()) {
            upsert(KEY_B2B, b2b.trim());
        }
        log.info("Commission rates updated: b2c={}, b2b={}", b2c, b2b);
        return getCommission();
    }

    private String readSetting(String key, String defaultValue) {
        return settingRepository.findBySettingKey(key)
                .map(AdminSetting::getSettingValue)
                .orElse(defaultValue);
    }

    private void upsert(String key, String value) {
        AdminSetting setting = settingRepository.findBySettingKey(key).orElseGet(() -> {
            AdminSetting created = new AdminSetting();
            created.setSettingKey(key);
            return created;
        });
        setting.setSettingValue(value);
        settingRepository.save(setting);
    }
}
