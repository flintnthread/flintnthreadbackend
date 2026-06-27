package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.config.TwilioProperties;
import com.ecommerce.sellerbackend.service.SmsService;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.Account;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwilioSmsService implements SmsService {

    private final TwilioProperties twilioProperties;

    @Value("${app.otp.dev-mode:false}")
    private boolean devMode;

    @Value("${app.otp.expiry-minutes:2}")
    private int otpExpiryMinutes;

    @PostConstruct
    void init() {
        if (devMode) {
            log.warn("app.otp.dev-mode=true — OTP will NOT be sent by SMS");
            return;
        }
        if (!hasTwilioCredentials()) {
            log.error(
                    "Twilio is not configured. Copy application-local.properties.example to "
                            + "application-local.properties and set twilio.account.sid, twilio.auth.token, "
                            + "twilio.phone.number");
            return;
        }
        log.info(
                "Twilio SMS enabled — account {}…{}, from {}",
                mask(twilioProperties.getAccountSid(), 4),
                tail(twilioProperties.getAccountSid(), 4),
                twilioProperties.getPhoneNumber());
        initTwilioClient();
        validateTwilioCredentials();
    }

    private void initTwilioClient() {
        if (twilioProperties.hasApiKey()) {
            Twilio.init(
                    twilioProperties.getAccountSid(),
                    twilioProperties.getApiKeySid(),
                    twilioProperties.getApiKeySecret());
            return;
        }
        Twilio.init(twilioProperties.getAccountSid(), twilioProperties.getAuthToken());
    }

    @Override
    public boolean isSmsConfigured() {
        return hasTwilioCredentials();
    }

    @Override
    public void sendOtp(String mobileE164, String otp) {
        if (devMode) {
            log.info("DEV OTP for {}: {}", mobileE164, otp);
            return;
        }
        if (!hasTwilioCredentials()) {
            throw new IllegalStateException(
                    "SMS is not configured. Set Twilio credentials in application-local.properties.");
        }
        initTwilioClient();

        String body = "Your Seller OTP is: " + otp + ".  This code is valid for " + otpExpiryMinutes
                + " minutes. Never share your OTP with anyone. Flint & Thread will never ask for your OTP via phone call, email, SMS, or customer support. If you did not request this OTP, please ignore this message. "
                + "Flint & Thread (India) Private Limited.";
        try {
            Message.creator(
                    new PhoneNumber(mobileE164),
                    new PhoneNumber(twilioProperties.getPhoneNumber()),
                    body
            ).create();
            log.info("OTP SMS sent to {}", mobileE164);
        } catch (ApiException ex) {
            log.error("Twilio SMS failed for {}: {} ({})", mobileE164, ex.getMessage(), ex.getCode());
            if (ex.getCode() == 20003) {
                throw new IllegalStateException(
                        "Twilio authentication failed. Regenerate Auth Token in Twilio Console and update application-local.properties.");
            }
            if (ex.getCode() == 21608 || ex.getCode() == 21211) {
                throw new IllegalStateException(
                        "This mobile number cannot receive SMS yet. On a Twilio trial account, verify the number in Twilio Console first.");
            }
            throw new IllegalStateException(
                    "Unable to send OTP SMS. Please check the mobile number or try again later.");
        }
    }

    private void validateTwilioCredentials() {
        try {
            Account.fetcher(twilioProperties.getAccountSid()).fetch();
            log.info("Twilio credentials verified for account {}", twilioProperties.getAccountSid());
        } catch (ApiException ex) {
            log.error(
                    "Twilio authentication failed at startup ({}). Update twilio.auth.token in application-local.properties",
                    ex.getCode());
        }
    }

    private boolean hasTwilioCredentials() {
        boolean hasSid = twilioProperties.getAccountSid() != null
                && !twilioProperties.getAccountSid().isBlank();
        boolean hasFrom = twilioProperties.getPhoneNumber() != null
                && !twilioProperties.getPhoneNumber().isBlank();
        boolean hasAuthToken = twilioProperties.getAuthToken() != null
                && !twilioProperties.getAuthToken().isBlank();
        boolean hasApiKey = twilioProperties.hasApiKey();
        return hasSid && hasFrom && (hasAuthToken || hasApiKey);
    }

    private static String mask(String value, int keepStart) {
        if (value == null || value.length() <= keepStart) {
            return "****";
        }
        return value.substring(0, keepStart);
    }

    private static String tail(String value, int keepEnd) {
        if (value == null || value.length() <= keepEnd) {
            return "****";
        }
        return value.substring(value.length() - keepEnd);
    }
}
