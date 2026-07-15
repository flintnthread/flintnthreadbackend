package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.AdminSetting;
import com.ecommerce.adminbackend.entity.Product;
import com.ecommerce.adminbackend.entity.ProductVariant;
import com.ecommerce.adminbackend.entity.Seller;
import com.ecommerce.adminbackend.entity.SellerCategory;
import com.ecommerce.adminbackend.repository.AdminSettingRepository;
import com.ecommerce.adminbackend.repository.ProductRepository;
import com.ecommerce.adminbackend.repository.ProductVariantRepository;
import com.ecommerce.adminbackend.repository.SellerRepository;
import com.ecommerce.adminbackend.service.PlatformIntegrationSettings;
import com.ecommerce.adminbackend.service.SettingsAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import com.ecommerce.adminbackend.service.support.ProductVariantCommissionSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductVariantCommissionSupport commissionSupport;

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
        boolean updatedB2c = false;
        boolean updatedB2b = false;
        if (b2c != null && !b2c.isBlank()) {
            upsert(KEY_B2C, b2c.trim());
            updatedB2c = true;
        }
        if (b2b != null && !b2b.isBlank()) {
            upsert(KEY_B2B, b2b.trim());
            updatedB2b = true;
        }
        log.info("Commission rates updated: b2c={}, b2b={}", b2c, b2b);

        if (updatedB2c) {
            int count = reapplyCommissionForCategory(
                    SellerCategory.b2c,
                    new BigDecimal(readSetting(KEY_B2C, DEFAULT_B2C).trim()));
            log.info("Reapplied B2C commission to {} product variants", count);
        }
        if (updatedB2b) {
            int count = reapplyCommissionForCategory(
                    SellerCategory.b2b,
                    new BigDecimal(readSetting(KEY_B2B, DEFAULT_B2B).trim()));
            log.info("Reapplied B2B commission to {} product variants", count);
        }
        return getCommission();
    }

    /**
     * Force-refresh commission % / amount / totals on all variants for sellers
     * in the given category — same pricing path used on product approval.
     */
    private int reapplyCommissionForCategory(SellerCategory category, BigDecimal commissionPercent) {
        List<Seller> sellers = sellerRepository.findBySellerCategory(category);
        // Sellers with null category are treated as B2C (profile default).
        if (category == SellerCategory.b2c) {
            List<Seller> all = sellerRepository.findAll();
            sellers = all.stream()
                    .filter(s -> s.getSellerCategory() == null || s.getSellerCategory() == SellerCategory.b2c)
                    .toList();
        }
        int updated = 0;
        for (Seller seller : sellers) {
            for (Product product : productRepository.findBySellerId(seller.getId())) {
                for (ProductVariant variant : productVariantRepository.findByProductIdOrderByIdAsc(product.getId())) {
                    commissionSupport.applyCommission(
                            variant, commissionPercent, product.getGstPercentage(), true);
                    productVariantRepository.save(variant);
                    updated++;
                }
            }
        }
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getIntegrations() {
        String sendGridKey = integrationSettings.getSendGridApiKey();
        String twilioSid = integrationSettings.getTwilioAccountSid();
        String twilioToken = integrationSettings.getTwilioAuthToken();
        String twilioPhone = integrationSettings.getTwilioPhoneNumber();
        String shiprocketEmail = integrationSettings.getShiprocketEmail();
        String shiprocketPassword = integrationSettings.getShiprocketPassword();
        String shiprocketPickup = integrationSettings.getShiprocketPickupLocation();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sendgridApiKeyConfigured", isConfigured(sendGridKey));
        response.put("sendgridApiKeyMasked", maskSecret(sendGridKey));
        response.put("twilioAccountSid", twilioSid);
        response.put("twilioAuthTokenConfigured", isConfigured(twilioToken));
        response.put("twilioAuthTokenMasked", maskSecret(twilioToken));
        response.put("twilioPhoneNumber", twilioPhone);
        response.put("shiprocketEmail", shiprocketEmail);
        response.put("shiprocketPasswordConfigured", isConfigured(shiprocketPassword));
        response.put("shiprocketPasswordMasked", maskSecret(shiprocketPassword));
        response.put("shiprocketPickupLocation", shiprocketPickup);
        return response;
    }

    @Override
    @Transactional
    public Map<String, Object> updateIntegrations(
            String sendgridApiKey,
            String twilioAccountSid,
            String twilioAuthToken,
            String twilioPhoneNumber,
            String shiprocketEmail,
            String shiprocketPassword,
            String shiprocketPickupLocation) {
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
        if (shiprocketEmail != null && !shiprocketEmail.isBlank()) {
            upsert(PlatformIntegrationSettings.KEY_SHIPROCKET_EMAIL, shiprocketEmail.trim());
        }
        if (shiprocketPassword != null && !shiprocketPassword.isBlank()) {
            upsert(PlatformIntegrationSettings.KEY_SHIPROCKET_PASSWORD, shiprocketPassword.trim());
        }
        if (shiprocketPickupLocation != null && !shiprocketPickupLocation.isBlank()) {
            upsert(PlatformIntegrationSettings.KEY_SHIPROCKET_PICKUP_LOCATION, shiprocketPickupLocation.trim());
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
