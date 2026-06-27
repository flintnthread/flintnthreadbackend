package com.ecommerce.sellerbackend.service.otp;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OtpVerificationStore {

    private final Map<String, OtpChallenge> byMobile = new ConcurrentHashMap<>();
    private final Map<String, String> mobileByToken = new ConcurrentHashMap<>();

    public void saveOtp(String mobileDigits, String otp, LocalDateTime expiresAt) {
        byMobile.put(mobileDigits, new OtpChallenge(mobileDigits, otp, expiresAt, null, null, false));
        mobileByToken.entrySet().removeIf(e -> e.getValue().equals(mobileDigits));
    }

    public Optional<OtpChallenge> findByMobile(String mobileDigits) {
        return Optional.ofNullable(byMobile.get(mobileDigits));
    }

    public String markVerified(String mobileDigits, LocalDateTime tokenExpiresAt) {
        OtpChallenge current = byMobile.get(mobileDigits);
        if (current == null) {
            return null;
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        OtpChallenge updated = new OtpChallenge(
                mobileDigits,
                current.otp(),
                current.otpExpiresAt(),
                token,
                tokenExpiresAt,
                true
        );
        byMobile.put(mobileDigits, updated);
        mobileByToken.put(token, mobileDigits);
        return token;
    }

    public Optional<String> peekVerificationToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String mobileDigits = mobileByToken.get(token);
        if (mobileDigits == null) {
            return Optional.empty();
        }
        OtpChallenge challenge = byMobile.get(mobileDigits);
        if (challenge == null || !challenge.verified() || challenge.verificationToken() == null) {
            return Optional.empty();
        }
        if (!token.equals(challenge.verificationToken())) {
            return Optional.empty();
        }
        if (challenge.tokenExpiresAt() == null || challenge.tokenExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        return Optional.of(mobileDigits);
    }

    public Optional<String> consumeVerificationToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Optional<String> mobileDigits = peekVerificationToken(token);
        if (mobileDigits.isEmpty()) {
            mobileByToken.remove(token);
            return Optional.empty();
        }
        mobileByToken.remove(token);
        byMobile.remove(mobileDigits.get());
        return mobileDigits;
    }
}
