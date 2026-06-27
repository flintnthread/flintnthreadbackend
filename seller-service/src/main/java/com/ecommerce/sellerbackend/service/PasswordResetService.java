package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.ForgotPasswordRequest;
import com.ecommerce.sellerbackend.dto.MessageResponse;
import com.ecommerce.sellerbackend.dto.ResetPasswordRequest;
import com.ecommerce.sellerbackend.dto.ResetTokenValidationResponse;

public interface PasswordResetService {

    MessageResponse requestPasswordReset(ForgotPasswordRequest request);

    ResetTokenValidationResponse validateResetToken(String token);

    MessageResponse resetPassword(ResetPasswordRequest request);
}
