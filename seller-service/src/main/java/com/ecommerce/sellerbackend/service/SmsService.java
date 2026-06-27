package com.ecommerce.sellerbackend.service;

public interface SmsService {

    void sendOtp(String mobileE164, String otp);

    /** True when a real SMS provider is configured and ready to send. */
    boolean isSmsConfigured();
}
