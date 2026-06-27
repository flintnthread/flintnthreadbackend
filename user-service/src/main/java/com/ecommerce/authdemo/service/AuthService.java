package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.AuthResponseDTO;
import com.ecommerce.authdemo.dto.LoginRequestDTO;
import com.ecommerce.authdemo.dto.OtpResponseDTO;
import com.ecommerce.authdemo.dto.VerifyOtpDTO;

public interface AuthService {

    String sendOtp(LoginRequestDTO dto);
    AuthResponseDTO verifyOtp(VerifyOtpDTO dto);
}

