package com.ecommerce.adminbackend.service;

import java.util.Map;

public interface SettingsAdminService {

    Map<String, String> getCommission();

    Map<String, String> updateCommission(String b2c, String b2b);

    Map<String, Object> getIntegrations();

    Map<String, Object> updateIntegrations(
            String sendgridApiKey,
            String twilioAccountSid,
            String twilioAuthToken,
            String twilioPhoneNumber);
}
