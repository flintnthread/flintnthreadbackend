package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.AuthResponseDTO;
import com.ecommerce.authdemo.dto.OtpResponseDTO;
import com.ecommerce.authdemo.dto.SignupCompleteDTO;
import com.ecommerce.authdemo.dto.SignupSendEmailOtpDTO;
import com.ecommerce.authdemo.dto.SignupSendPhoneOtpDTO;
import com.ecommerce.authdemo.dto.SignupVerifyEmailOtpDTO;
import com.ecommerce.authdemo.dto.SignupVerifyPhoneOtpDTO;
import com.ecommerce.authdemo.exception.*;
import com.ecommerce.authdemo.service.SignupService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth/signup")
@CrossOrigin("*")
public class SignupController {

    @Autowired
    private SignupService signupService;

    @PostMapping("/send-email-otp")
    public ResponseEntity<?> sendEmailOtp(@Valid @RequestBody SignupSendEmailOtpDTO dto) {
        return handleOtpSend(() -> signupService.sendEmailOtp(dto));
    }

    @PostMapping("/verify-email-otp")
    public ResponseEntity<?> verifyEmailOtp(@Valid @RequestBody SignupVerifyEmailOtpDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = signupService.verifyEmailOtp(dto);
            return ResponseEntity.ok(result);
        } catch (SignupException e) {
            response.put("success", false);
            response.put("code", e.getCode());
            response.put("message", e.getMessage());
            HttpStatus status = "INVALID_OTP".equals(e.getCode())
                    ? HttpStatus.BAD_REQUEST
                    : HttpStatus.CONFLICT;
            return ResponseEntity.status(status).body(response);
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

    @PostMapping("/send-phone-otp")
    public ResponseEntity<?> sendPhoneOtp(@Valid @RequestBody SignupSendPhoneOtpDTO dto) {
        return handleOtpSend(() -> signupService.sendPhoneOtp(dto));
    }

    @PostMapping("/verify-phone-otp")
    public ResponseEntity<?> verifyPhoneOtp(@Valid @RequestBody SignupVerifyPhoneOtpDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            return ResponseEntity.ok(signupService.verifyPhoneOtp(dto));
        } catch (SignupException e) {
            response.put("success", false);
            response.put("code", e.getCode());
            response.put("message", e.getMessage());
            HttpStatus status = "INVALID_OTP".equals(e.getCode())
                    ? HttpStatus.BAD_REQUEST
                    : HttpStatus.CONFLICT;
            return ResponseEntity.status(status).body(response);
        } catch (InvalidMobileException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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

    @PostMapping("/complete")
    public ResponseEntity<?> completeSignup(@Valid @RequestBody SignupCompleteDTO dto) {
        Map<String, Object> response = new HashMap<>();
        try {
            AuthResponseDTO auth = signupService.completeSignup(dto);
            response.put("success", true);
            response.put("token", auth.getToken());
            response.put("role", auth.getRole());
            response.put("userId", auth.getUserId());
            response.put("email", auth.getEmail());
            response.put("contactNumber", auth.getContactNumber());
            response.put("displayName", auth.getDisplayName());
            response.put("message", "Account created successfully");
            return ResponseEntity.ok(response);
        } catch (SignupException e) {
            response.put("success", false);
            response.put("code", e.getCode());
            response.put("message", e.getMessage());
            HttpStatus status = "INVALID_OTP".equals(e.getCode())
                    ? HttpStatus.BAD_REQUEST
                    : HttpStatus.CONFLICT;
            return ResponseEntity.status(status).body(response);
        } catch (InvalidMobileException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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

    private ResponseEntity<?> handleOtpSend(OtpSendCall call) {
        Map<String, Object> response = new HashMap<>();
        try {
            OtpResponseDTO result = call.run();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            if (result.getDeliveryChannel() != null) {
                response.put("deliveryChannel", result.getDeliveryChannel());
            }
            if (result.getNextStep() != null) {
                response.put("nextStep", result.getNextStep());
            }
            if (result.getEmail() != null) {
                response.put("email", result.getEmail());
            }
            if (result.getMaskedPhone() != null) {
                response.put("maskedPhone", result.getMaskedPhone());
            }
            return ResponseEntity.ok(response);
        } catch (SignupException e) {
            response.put("success", false);
            response.put("code", e.getCode());
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (InvalidMobileException | InvalidEmailException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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

    @FunctionalInterface
    private interface OtpSendCall {
        OtpResponseDTO run();
    }
}
