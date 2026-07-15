package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.cms.SiteSetting;
import com.ecommerce.adminbackend.repository.SiteSettingRepository;
import com.ecommerce.adminbackend.service.HomepageSectionAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HomepageSectionAdminServiceImpl extends BaseAdminService implements HomepageSectionAdminService {

    private final SiteSettingRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return repository.findHomepageSectionSettings().stream().map(this::toMap).toList();
    }

    @Override
    @Transactional
    public List<Map<String, Object>> upsert(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one {key,value} setting is required.");
        }
        List<Map<String, Object>> saved = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String key = firstNonBlank(stringAt(item, "key"), stringAt(item, "settingKey"), stringAt(item, "setting_key"));
            String value = firstNonBlank(stringAt(item, "value"), stringAt(item, "settingValue"), stringAt(item, "setting_value"));
            requireNonBlank(key, "key");
            if (!key.startsWith("homepage_sec_") && !key.startsWith("homepage_show_")) {
                throw new IllegalArgumentException(
                        "Only homepage_sec_* or homepage_show_* keys can be updated via this endpoint: " + key);
            }
            if (value == null) {
                value = "";
            }
            if (value.length() > 500) {
                throw new IllegalArgumentException("setting value exceeds 500 characters for key: " + key);
            }
            final String settingValue = value;
            SiteSetting setting = repository.findBySettingKey(key).orElseGet(SiteSetting::new);
            setting.setSettingKey(key);
            setting.setSettingValue(settingValue);
            saved.add(toMap(repository.save(setting)));
        }
        return saved;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private Map<String, Object> toMap(SiteSetting setting) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", setting.getId());
        row.put("key", setting.getSettingKey());
        row.put("value", setting.getSettingValue());
        row.put("updatedAt", setting.getUpdatedAt());
        return row;
    }
}
