package com.ecommerce.sellerbackend;

import com.ecommerce.sellerbackend.dto.RegisterSellerRequest;
import com.ecommerce.sellerbackend.dto.RegisterSellerResponse;
import com.ecommerce.sellerbackend.repository.SellerRepository;
import com.ecommerce.sellerbackend.service.SellerRegistrationService;
import com.ecommerce.sellerbackend.service.otp.OtpVerificationStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RegistrationIntegrationTest {

    @Autowired
    private SellerRegistrationService registrationService;

    @Autowired
    private OtpVerificationStore otpStore;

    @Autowired
    private SellerRepository sellerRepository;

    @Test
    @Transactional
    void register_persistsSellerToDatabase() {
        String mobile = "70000" + String.format("%05d", System.currentTimeMillis() % 100000);
        String email = "regtest" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        otpStore.saveOtp(mobile, "123456", LocalDateTime.now().plusMinutes(5));
        String token = otpStore.markVerified(mobile, LocalDateTime.now().plusMinutes(30));
        assertNotNull(token);

        RegisterSellerRequest request = new RegisterSellerRequest();
        request.setMobileVerificationToken(token);
        request.setMobile(mobile);
        request.setFirstName("Test");
        request.setLastName("Seller");
        request.setEmail(email);
        request.setPassword("Test@12345");
        request.setConfirmPassword("Test@12345");

        RegisterSellerResponse response = registrationService.register(request);

        assertNotNull(response.getSellerId());
        var saved = sellerRepository.findById(response.getSellerId());
        assertTrue(saved.isPresent());
        assertNotNull(saved.get().getEmailVerificationToken());
        assertNull(saved.get().getOtp());
    }
}
