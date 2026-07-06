package com.ecommerce.adminbackend.dto.settings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IntegrationSettingsRequest {
    private String sendgridApiKey;
    private String twilioAccountSid;
    private String twilioAuthToken;
    private String twilioPhoneNumber;
}
