package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.OtpSentResponse;
import com.ecommerce.sellerbackend.dto.OtpVerifiedResponse;
import com.ecommerce.sellerbackend.dto.RegisterSellerRequest;
import com.ecommerce.sellerbackend.dto.RegisterSellerResponse;
import com.ecommerce.sellerbackend.dto.SendOtpRequest;
import com.ecommerce.sellerbackend.dto.VerifyOtpRequest;

public interface SellerRegistrationService {

    OtpSentResponse sendOtp(SendOtpRequest request);

    OtpVerifiedResponse verifyOtp(VerifyOtpRequest request);

    RegisterSellerResponse register(RegisterSellerRequest request);
}
