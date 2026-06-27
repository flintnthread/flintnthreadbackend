package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.OtpSentResponse;
import com.ecommerce.sellerbackend.dto.OtpVerifiedResponse;
import com.ecommerce.sellerbackend.dto.RegisterSellerRequest;
import com.ecommerce.sellerbackend.dto.RegisterSellerResponse;
import com.ecommerce.sellerbackend.dto.SendOtpRequest;
import com.ecommerce.sellerbackend.dto.VerifyOtpRequest;
import com.ecommerce.sellerbackend.service.SellerRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
public class SellerRegistrationController {

    private final SellerRegistrationService sellerRegistrationService;

    @PostMapping("/send-otp")
    public OtpSentResponse sendOtp(@Valid @RequestBody SendOtpRequest request) {
        return sellerRegistrationService.sendOtp(request);
    }

    @PostMapping("/verify-otp")
    public OtpVerifiedResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return sellerRegistrationService.verifyOtp(request);
    }

    @PostMapping("/register")
    public RegisterSellerResponse register(@Valid @RequestBody RegisterSellerRequest request) {
        return sellerRegistrationService.register(request);
    }
}
