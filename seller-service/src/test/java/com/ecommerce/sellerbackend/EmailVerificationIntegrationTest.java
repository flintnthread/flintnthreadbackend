package com.ecommerce.sellerbackend;

import com.ecommerce.sellerbackend.dto.RegisterSellerRequest;
import com.ecommerce.sellerbackend.dto.RegisterSellerResponse;
import com.ecommerce.sellerbackend.dto.StartEmailVerificationRequest;
import com.ecommerce.sellerbackend.dto.VerifyEmailOtpRequest;
import com.ecommerce.sellerbackend.entity.SellerAccountStatus;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import com.ecommerce.sellerbackend.service.EmailVerificationService;
import com.ecommerce.sellerbackend.service.SellerRegistrationService;
import com.ecommerce.sellerbackend.service.otp.OtpVerificationStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class EmailVerificationIntegrationTest {

    @Autowired
    private SellerRegistrationService registrationService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private OtpVerificationStore otpStore;

    @Autowired
    private SellerRepository sellerRepository;

    @Test
    @Transactional
    void emailVerification_linkClickThenOtpVerify() {
        String mobile = "80000" + String.format("%05d", System.currentTimeMillis() % 100000);
        String email = "emailverify" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        otpStore.saveOtp(mobile, "123456", LocalDateTime.now().plusMinutes(5));
        String mobileToken = otpStore.markVerified(mobile, LocalDateTime.now().plusMinutes(30));
        assertNotNull(mobileToken);

        RegisterSellerRequest registerRequest = new RegisterSellerRequest();
        registerRequest.setMobileVerificationToken(mobileToken);
        registerRequest.setMobile(mobile);
        registerRequest.setFirstName("Sandhya");
        registerRequest.setLastName("Test");
        registerRequest.setEmail(email);
        registerRequest.setPassword("Test@12345");
        registerRequest.setConfirmPassword("Test@12345");

        RegisterSellerResponse registerResponse = registrationService.register(registerRequest);
        assertTrue(registerResponse.isEmailVerificationRequired());

        var seller = sellerRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertNotNull(seller.getEmailVerificationToken());
        assertFalse(Boolean.TRUE.equals(seller.getEmailVerified()));

        StartEmailVerificationRequest linkRequest = new StartEmailVerificationRequest();
        linkRequest.setToken(seller.getEmailVerificationToken());
        var startResponse = emailVerificationService.confirmEmailLink(linkRequest);
        assertTrue(startResponse.isOtpSent());
        assertEquals(email, startResponse.getEmail());

        seller = sellerRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertNotNull(seller.getOtp());
        assertNotNull(seller.getOtpExpiresAt());

        VerifyEmailOtpRequest otpRequest = new VerifyEmailOtpRequest();
        otpRequest.setEmail(email);
        otpRequest.setOtp(seller.getOtp());
        var verifyResponse = emailVerificationService.verifyEmailOtp(otpRequest);
        assertTrue(verifyResponse.isVerified());

        seller = sellerRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertTrue(Boolean.TRUE.equals(seller.getEmailVerified()));
        assertEquals(SellerAccountStatus.active, seller.getStatus());
    }
}
