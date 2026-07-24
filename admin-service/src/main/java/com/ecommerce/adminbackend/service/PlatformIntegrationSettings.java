package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.logging.LogFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlatformIntegrationSettings {

    private static final Logger log = LogFactory.getLogger(PlatformIntegrationSettings.class);

    public static final String KEY_SENDGRID_API_KEY = "sendgrid_api_key";
    public static final String KEY_TWILIO_ACCOUNT_SID = "twilio_account_sid";
    public static final String KEY_TWILIO_AUTH_TOKEN = "twilio_auth_token";
    public static final String KEY_TWILIO_PHONE_NUMBER = "twilio_phone_number";
    public static final String KEY_SHIPROCKET_EMAIL = "shiprocket_email";
    public static final String KEY_SHIPROCKET_PASSWORD = "shiprocket_password";
    public static final String KEY_SHIPROCKET_PICKUP_LOCATION = "shiprocket_pickup_location";

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.mail.password:}")
    private String defaultSendGridApiKey;

    @Value("${twilio.account.sid:}")
    private String defaultTwilioAccountSid;

    @Value("${twilio.auth.token:}")
    private String defaultTwilioAuthToken;

    @Value("${twilio.phone.number:}")
    private String defaultTwilioPhoneNumber;

    @Value("${shiprocket.email:}")
    private String defaultShiprocketEmail;

    @Value("${shiprocket.password:}")
    private String defaultShiprocketPassword;

    @Value("${shiprocket.pickup-location:work}")
    private String defaultShiprocketPickupLocation;

    public String getSendGridApiKey() {
        return readSetting(KEY_SENDGRID_API_KEY).orElse(trimToEmpty(defaultSendGridApiKey));
    }

    public String getTwilioAccountSid() {
        return readSetting(KEY_TWILIO_ACCOUNT_SID).orElse(trimToEmpty(defaultTwilioAccountSid));
    }

    public String getTwilioAuthToken() {
        return readSetting(KEY_TWILIO_AUTH_TOKEN).orElse(trimToEmpty(defaultTwilioAuthToken));
    }

    public String getTwilioPhoneNumber() {
        return readSetting(KEY_TWILIO_PHONE_NUMBER).orElse(trimToEmpty(defaultTwilioPhoneNumber));
    }

    public String getShiprocketEmail() {
        return readSetting(KEY_SHIPROCKET_EMAIL).orElse(trimToEmpty(defaultShiprocketEmail));
    }

    public String getShiprocketPassword() {
        return readSetting(KEY_SHIPROCKET_PASSWORD).orElse(trimToEmpty(defaultShiprocketPassword));
    }

    public String getShiprocketPickupLocation() {
        return readSetting(KEY_SHIPROCKET_PICKUP_LOCATION).orElse(trimToEmpty(defaultShiprocketPickupLocation));
    }

    public Optional<String> readSetting(String key) {
        try {
            return jdbcTemplate.query(
                            "SELECT setting_value FROM admin_settings WHERE setting_key = ? LIMIT 1",
                            (rs, rowNum) -> rs.getString(1),
                            key)
                    .stream()
                    .findFirst()
                    .map(String::trim)
                    .filter(value -> !value.isBlank());
        } catch (Exception ex) {
            log.warn("Could not read admin setting {}: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
