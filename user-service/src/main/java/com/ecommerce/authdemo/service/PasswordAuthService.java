package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.AuthResponseDTO;
import com.ecommerce.authdemo.dto.ForgotPasswordResetDTO;
import com.ecommerce.authdemo.dto.ForgotPasswordSendOtpDTO;
import com.ecommerce.authdemo.dto.ForgotPasswordVerifyOtpDTO;
import com.ecommerce.authdemo.dto.OtpResponseDTO;
import com.ecommerce.authdemo.dto.PasswordLoginDTO;

import java.util.Map;

public interface PasswordAuthService {

    AuthResponseDTO login(PasswordLoginDTO dto);

    OtpResponseDTO sendForgotPasswordOtp(ForgotPasswordSendOtpDTO dto);

    Map<String, Object> verifyForgotPasswordOtp(ForgotPasswordVerifyOtpDTO dto);

    Map<String, Object> resetPassword(ForgotPasswordResetDTO dto);
}
