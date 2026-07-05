package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.AdminSetting;
import com.ecommerce.adminbackend.repository.AdminSettingRepository;
import com.ecommerce.adminbackend.service.PlatformIntegrationSettings;
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
    private final PlatformIntegrationSettings integrationSettings;

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

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getIntegrations() {
        String sendGridKey = integrationSettings.getSendGridApiKey();
        String twilioSid = integrationSettings.getTwilioAccountSid();
        String twilioToken = integrationSettings.getTwilioAuthToken();
        String twilioPhone = integrationSettings.getTwilioPhoneNumber();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sendgridApiKeyConfigured", isConfigured(sendGridKey));
        response.put("sendgridApiKeyMasked", maskSecret(sendGridKey));
        response.put("twilioAccountSid", twilioSid);
        response.put("twilioAuthTokenConfigured", isConfigured(twilioToken));
        response.put("twilioAuthTokenMasked", maskSecret(twilioToken));
        response.put("twilioPhoneNumber", twilioPhone);
        return response;
    }

    @Override
    @Transactional
    public Map<String, Object> updateIntegrations(
            String sendgridApiKey,
            String twilioAccountSid,
            String twilioAuthToken,
            String twilioPhoneNumber) {
        if (sendgridApiKey != null && !sendgridApiKey.isBlank()) {
            upsert(PlatformIntegrationSettings.KEY_SENDGRID_API_KEY, sendgridApiKey.trim());
        }
        if (twilioAccountSid != null && !twilioAccountSid.isBlank()) {
            upsert(PlatformIntegrationSettings.KEY_TWILIO_ACCOUNT_SID, twilioAccountSid.trim());
        }
        if (twilioAuthToken != null && !twilioAuthToken.isBlank()) {
            upsert(PlatformIntegrationSettings.KEY_TWILIO_AUTH_TOKEN, twilioAuthToken.trim());
        }
        if (twilioPhoneNumber != null && !twilioPhoneNumber.isBlank()) {
            upsert(PlatformIntegrationSettings.KEY_TWILIO_PHONE_NUMBER, twilioPhoneNumber.trim());
        }
        log.info("Integration settings updated from admin panel");
        return getIntegrations();
    }

    private boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }

    private String maskSecret(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
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
