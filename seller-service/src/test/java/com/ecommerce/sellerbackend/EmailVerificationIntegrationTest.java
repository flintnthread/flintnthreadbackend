package com.ecommerce.sellerbackend;

import com.ecommerce.sellerbackend.dto.RegisterSellerRequest;
import com.ecommerce.sellerbackend.dto.RegisterSellerResponse;
import com.ecommerce.sellerbackend.dto.StartEmailVerificationRequest;
import com.ecommerce.sellerbackend.dto.VerifyEmailOtpRequest;
import com.ecommerce.sellerbackend.entity.SellerAccountStatus;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import com.ecommerce.sellerbackend.service.EmailVerificationService;
import com.ecommerce.sellerbackend.service.MailService;
import com.ecommerce.sellerbackend.service.SellerRegistrationService;
import com.ecommerce.sellerbackend.service.otp.OtpVerificationStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

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

    @MockBean
    private MailService mailService;

    @Test
    @Transactional
    void emailVerification_linkClickSendsOtpThenOtpActivatesAccount() {
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
        verify(mailService, atLeastOnce()).sendEmailVerificationLinkEmail(anyString(), anyString(), anyString());

        var seller = sellerRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertNotNull(seller.getEmailVerificationToken());
        assertFalse(Boolean.TRUE.equals(seller.getEmailVerified()));
        assertNull(seller.getOtp());

        StartEmailVerificationRequest linkRequest = new StartEmailVerificationRequest();
        linkRequest.setToken(seller.getEmailVerificationToken());
        var startResponse = emailVerificationService.confirmEmailLink(linkRequest);
        assertTrue(startResponse.isOtpSent());
        assertFalse(startResponse.isAlreadyVerified());
        assertEquals(email, startResponse.getEmail());
        verify(mailService, atLeastOnce()).sendEmailVerificationOtpEmail(anyString(), anyString(), anyString());

        seller = sellerRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertFalse(Boolean.TRUE.equals(seller.getEmailVerified()));
        assertNotNull(seller.getOtp());
        assertNotNull(seller.getEmailVerificationToken());

        VerifyEmailOtpRequest otpRequest = new VerifyEmailOtpRequest();
        otpRequest.setEmail(email);
        otpRequest.setOtp(seller.getOtp());
        var verified = emailVerificationService.verifyEmailOtp(otpRequest);
        assertTrue(verified.isVerified());

        seller = sellerRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertTrue(Boolean.TRUE.equals(seller.getEmailVerified()));
        assertNull(seller.getEmailVerificationToken());
        assertNull(seller.getOtp());
        assertEquals(SellerAccountStatus.active, seller.getStatus());
    }
}
