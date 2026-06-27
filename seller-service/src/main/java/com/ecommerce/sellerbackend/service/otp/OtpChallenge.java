package com.ecommerce.sellerbackend.service.otp;

import java.time.LocalDateTime;

public record OtpChallenge(
        String mobileDigits,
        String otp,
        LocalDateTime otpExpiresAt,
        String verificationToken,
        LocalDateTime tokenExpiresAt,
        boolean verified
) {
}
