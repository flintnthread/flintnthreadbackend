package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.AuthResponseDTO;
import com.ecommerce.authdemo.dto.ForgotPasswordResetDTO;
import com.ecommerce.authdemo.dto.ForgotPasswordSendOtpDTO;
import com.ecommerce.authdemo.dto.ForgotPasswordVerifyOtpDTO;
import com.ecommerce.authdemo.dto.OtpResponseDTO;
import com.ecommerce.authdemo.dto.PasswordLoginDTO;
import com.ecommerce.authdemo.exception.*;
import com.ecommerce.authdemo.service.PasswordAuthService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin("*")
public class PasswordAuthController {

    @Autowired
    private PasswordAuthService passwordAuthService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody PasswordLoginDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            AuthResponseDTO auth = passwordAuthService.login(dto);
            response.put("success", true);
            response.put("token", auth.getToken());
            response.put("role", auth.getRole());
            response.put("userId", auth.getUserId());
            response.put("email", auth.getEmail());
            response.put("contactNumber", auth.getContactNumber());
            response.put("displayName", auth.getDisplayName());
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        } catch (AuthException e) {
            response.put("success", false);
            response.put("code", e.getCode());
            response.put("message", e.getMessage());
            return ResponseEntity.status(e.getHttpStatus()).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<?> sendForgotOtp(@Valid @RequestBody ForgotPasswordSendOtpDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            OtpResponseDTO result = passwordAuthService.sendForgotPasswordOtp(dto);
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            if (result.getDeliveryChannel() != null) {
                response.put("deliveryChannel", result.getDeliveryChannel());
            }
            if (result.getEmail() != null) response.put("email", result.getEmail());
            if (result.getMaskedPhone() != null) response.put("maskedPhone", result.getMaskedPhone());
            if (result.getNextStep() != null) response.put("nextStep", result.getNextStep());
            return ResponseEntity.ok(response);
        } catch (AuthException e) {
            response.put("success", false);
            response.put("code", e.getCode());
            response.put("message", e.getMessage());
            return ResponseEntity.status(e.getHttpStatus()).body(response);
        } catch (TooManyRequestsException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        } catch (EmailSendException | SmsSendException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<?> verifyForgotOtp(@Valid @RequestBody ForgotPasswordVerifyOtpDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            return ResponseEntity.ok(passwordAuthService.verifyForgotPasswordOtp(dto));
        } catch (AuthException e) {
            response.put("success", false);
            response.put("code", e.getCode());
            response.put("message", e.getMessage());
            return ResponseEntity.status(e.getHttpStatus()).body(response);
        } catch (OtpNotFoundException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (OtpExpiredException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.GONE).body(response);
        } catch (TooManyAttemptsException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ForgotPasswordResetDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            return ResponseEntity.ok(passwordAuthService.resetPassword(dto));
        } catch (AuthException e) {
            response.put("success", false);
            response.put("code", e.getCode());
            response.put("message", e.getMessage());
            return ResponseEntity.status(e.getHttpStatus()).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
