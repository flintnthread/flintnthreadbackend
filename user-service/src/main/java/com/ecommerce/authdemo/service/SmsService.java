package com.ecommerce.authdemo.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final PlatformIntegrationSettings integrationSettings;

    public void sendSms(String toNumber, String message) {
        if (!isConfigured()) {
            log.warn("Twilio is not configured; SMS not sent to {}", toNumber);
            return;
        }

        Twilio.init(integrationSettings.getTwilioAccountSid(), integrationSettings.getTwilioAuthToken());

        Message.creator(
                new PhoneNumber(toNumber),
                new PhoneNumber(integrationSettings.getTwilioPhoneNumber()),
                message
        ).create();
    }

    private boolean isConfigured() {
        String accountSid = integrationSettings.getTwilioAccountSid();
        String authToken = integrationSettings.getTwilioAuthToken();
        String fromNumber = integrationSettings.getTwilioPhoneNumber();
        return accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank()
                && fromNumber != null && !fromNumber.isBlank();
    }
}
