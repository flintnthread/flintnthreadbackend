package com.ecommerce.sellerbackend.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "twilio")
@Slf4j
public class TwilioProperties {

    private Account account = new Account();
    private Auth auth = new Auth();
    private Phone phone = new Phone();
    private ApiKey apiKey = new ApiKey();

    @PostConstruct
    void loadFromDotEnv() {
        for (Path path : List.of(
                Path.of(".env"),
                Path.of("seller-backend", ".env"),
                Path.of(System.getProperty("user.dir", "."), ".env"),
                Path.of(System.getProperty("user.dir", "."), "seller-backend", ".env"))) {
            if (!Files.isRegularFile(path)) {
                continue;
            }
            applyFromFile(path);
            if (hasCredentials()) {
                log.info(
                        "Twilio config from {} — account {}…{}, from {}",
                        path.toAbsolutePath().normalize(),
                        mask(getAccountSid(), 4),
                        tail(getAccountSid(), 4),
                        getPhoneNumber());
                return;
            }
        }
        if (!hasCredentials()) {
            log.warn("No Twilio credentials found. Set twilio.* in seller-backend/.env and restart.");
        }
    }

    public String getAccountSid() {
        String fromProps = account != null ? account.getSid() : null;
        if (isSet(fromProps)) {
            return fromProps.trim();
        }
        String fromEnv = System.getenv("TWILIO_ACCOUNT_SID");
        return isSet(fromEnv) ? fromEnv.trim() : "";
    }

    public String getAuthToken() {
        String fromProps = auth != null ? auth.getToken() : null;
        if (isSet(fromProps)) {
            return fromProps.trim();
        }
        String fromEnv = System.getenv("TWILIO_AUTH_TOKEN");
        return isSet(fromEnv) ? fromEnv.trim() : "";
    }

    public String getPhoneNumber() {
        String fromProps = phone != null ? phone.getNumber() : null;
        if (isSet(fromProps)) {
            return fromProps.trim();
        }
        String fromEnv = System.getenv("TWILIO_PHONE_NUMBER");
        return isSet(fromEnv) ? fromEnv.trim() : "";
    }

    public String getApiKeySid() {
        return apiKey != null && apiKey.getSid() != null ? apiKey.getSid().trim() : "";
    }

    public String getApiKeySecret() {
        return apiKey != null && apiKey.getSecret() != null ? apiKey.getSecret().trim() : "";
    }

    public boolean hasApiKey() {
        return isSet(getApiKeySid()) && isSet(getApiKeySecret());
    }

    public boolean hasCredentials() {
        return isSet(getAccountSid())
                && isSet(getPhoneNumber())
                && (isSet(getAuthToken()) || hasApiKey());
    }

    private void applyFromFile(Path path) {
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                if (!isSet(value)) {
                    continue;
                }
                switch (key) {
                    case "twilio.account.sid" -> account.setSid(value);
                    case "twilio.auth.token" -> auth.setToken(value);
                    case "twilio.phone.number" -> phone.setNumber(value);
                    case "twilio.api.key.sid" -> apiKey.setSid(value);
                    case "twilio.api.key.secret" -> apiKey.setSecret(value);
                    default -> { }
                }
            }
        } catch (Exception ex) {
            log.debug("Could not read {}: {}", path, ex.getMessage());
        }
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

    private static boolean isSet(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim();
        return !normalized.startsWith("YOUR_")
                && !normalized.equalsIgnoreCase("your_auth_token_here")
                && !normalized.equalsIgnoreCase("PASTE_YOUR_NEW_AUTH_TOKEN_HERE")
                && !normalized.contains("XXXXXXXXXX");
    }

    @Getter
    @Setter
    public static class Account {
        private String sid;
    }

    @Getter
    @Setter
    public static class Auth {
        private String token;
    }

    @Getter
    @Setter
    public static class Phone {
        private String number;
    }

    @Getter
    @Setter
    public static class ApiKey {
        private String sid;
        private String secret;
    }
}
