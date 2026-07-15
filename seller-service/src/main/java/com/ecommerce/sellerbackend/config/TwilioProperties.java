package com.ecommerce.sellerbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Legacy optional Twilio property binder. OTP SMS credentials are loaded from
 * Admin Platform Settings ({@code admin_settings}) via {@link com.ecommerce.sellerbackend.service.PlatformIntegrationSettings}.
 * These properties are unused for sending and kept only for compatibility.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "twilio")
public class TwilioProperties {

    private Account account = new Account();
    private Auth auth = new Auth();
    private Phone phone = new Phone();
    private ApiKey apiKey = new ApiKey();

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
