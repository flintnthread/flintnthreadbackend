package com.ecommerce.authdemo.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Value("${twilio.account.sid:}")
    private String accountSid;

    @Value("${twilio.auth.token:}")
    private String authToken;

    @Value("${twilio.phone.number:}")
    private String fromNumber;

    public void sendSms(String toNumber, String message) {
        if (!isConfigured()) {
            log.warn("Twilio is not configured; SMS not sent to {}", toNumber);
            return;
        }

        Twilio.init(accountSid, authToken);

        Message.creator(
                new PhoneNumber(toNumber),
                new PhoneNumber(fromNumber),
                message
        ).create();
    }

    private boolean isConfigured() {
        return accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank()
                && fromNumber != null && !fromNumber.isBlank();
    }
}
