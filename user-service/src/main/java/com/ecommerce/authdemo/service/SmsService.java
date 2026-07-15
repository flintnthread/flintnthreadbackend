package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.exception.SmsSendException;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Customer OTP SMS — credentials from Admin Platform Settings
 * ({@code admin_settings} via {@link PlatformIntegrationSettings}), same as seller-service.
 */
@Service
@RequiredArgsConstructor
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);
    private static final String ADMIN_SETTINGS_HINT =
            "Set Twilio Account SID, Auth Token, and Phone Number in Admin → Platform Settings.";

    private final PlatformIntegrationSettings integrationSettings;

    public boolean isConfigured() {
        String accountSid = integrationSettings.getTwilioAccountSid();
        String authToken = integrationSettings.getTwilioAuthToken();
        String fromNumber = integrationSettings.getTwilioPhoneNumber();
        return isSet(accountSid) && isSet(authToken) && isSet(fromNumber);
    }

    public void sendSms(String toNumber, String message) {
        if (!isConfigured()) {
            log.warn("Twilio is not configured; SMS not sent to {}. {}", toNumber, ADMIN_SETTINGS_HINT);
            throw new SmsSendException("SMS is not configured. " + ADMIN_SETTINGS_HINT);
        }

        Twilio.init(integrationSettings.getTwilioAccountSid(), integrationSettings.getTwilioAuthToken());

        try {
            Message.creator(
                    new PhoneNumber(toNumber),
                    new PhoneNumber(integrationSettings.getTwilioPhoneNumber()),
                    message
            ).create();
            log.info("SMS sent to {}", toNumber);
        } catch (ApiException ex) {
            log.error("Twilio SMS failed for {}: {} ({})", toNumber, ex.getMessage(), ex.getCode());
            if (ex.getCode() == 20003) {
                throw new SmsSendException(
                        "Twilio authentication failed. Update Auth Token in Admin → Platform Settings.");
            }
            if (ex.getCode() == 21608 || ex.getCode() == 21211) {
                throw new SmsSendException(
                        "This mobile number cannot receive SMS yet. On a Twilio trial account, verify the number in Twilio Console first.");
            }
            throw new SmsSendException(
                    "Unable to send OTP SMS. Please check the mobile number or try again later.");
        }
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
