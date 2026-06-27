package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.EmailVerificationResponse;
import com.ecommerce.sellerbackend.dto.MessageResponse;
import com.ecommerce.sellerbackend.dto.ResendEmailOtpRequest;
import com.ecommerce.sellerbackend.dto.StartEmailVerificationRequest;
import com.ecommerce.sellerbackend.dto.StartEmailVerificationResponse;
import com.ecommerce.sellerbackend.dto.VerifyEmailOtpRequest;

public interface EmailVerificationService {

    StartEmailVerificationResponse confirmEmailLink(StartEmailVerificationRequest request);

    EmailVerificationResponse verifyEmailOtp(VerifyEmailOtpRequest request);

    MessageResponse resendEmailOtp(ResendEmailOtpRequest request);
}
