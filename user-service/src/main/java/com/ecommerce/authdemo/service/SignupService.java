package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.AuthResponseDTO;
import com.ecommerce.authdemo.dto.OtpResponseDTO;
import com.ecommerce.authdemo.dto.SignupCompleteDTO;
import com.ecommerce.authdemo.dto.SignupSendEmailOtpDTO;
import com.ecommerce.authdemo.dto.SignupSendPhoneOtpDTO;
import com.ecommerce.authdemo.dto.SignupVerifyEmailOtpDTO;

import java.util.Map;

public interface SignupService {

    OtpResponseDTO sendEmailOtp(SignupSendEmailOtpDTO dto);

    Map<String, Object> verifyEmailOtp(SignupVerifyEmailOtpDTO dto);

    OtpResponseDTO sendPhoneOtp(SignupSendPhoneOtpDTO dto);

    AuthResponseDTO completeSignup(SignupCompleteDTO dto);
}
