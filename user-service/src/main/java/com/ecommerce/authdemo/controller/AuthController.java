package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.AuthResponseDTO;
import com.ecommerce.authdemo.dto.LoginRequestDTO;
import com.ecommerce.authdemo.dto.OtpResponseDTO;
import com.ecommerce.authdemo.dto.VerifyOtpDTO;
import com.ecommerce.authdemo.exception.*;
import com.ecommerce.authdemo.service.AuthService;

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
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody LoginRequestDTO dto) {

        Map<String, Object> response = new HashMap<>();

        try {
            OtpResponseDTO result = authService.sendOtp(dto);

            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            if (result.getDeliveryChannel() != null) {
                response.put("deliveryChannel", result.getDeliveryChannel());
            }
            if (result.getNextStep() != null) {
                response.put("nextStep", result.getNextStep());
            }
            if (result.getCode() != null) {
                response.put("code", result.getCode());
            }
            if (result.getEmail() != null) {
                response.put("email", result.getEmail());
            }
            if (result.getMaskedPhone() != null) {
                response.put("maskedPhone", result.getMaskedPhone());
            }

            return ResponseEntity.ok(response);

        } catch (InvalidMobileException | InvalidEmailException | InvalidIdentifierException e) {

            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (PhoneAlreadyLinkedException e) {

            response.put("success", false);
            response.put("code", "PHONE_ALREADY_LINKED");
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

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

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpDTO dto) {

        Map<String, Object> response = new HashMap<>();

        try {

            AuthResponseDTO authResponse = authService.verifyOtp(dto);

            response.put("success", true);
            response.put("token", authResponse.getToken());
            response.put("role", authResponse.getRole());
            if (authResponse.getUserId() != null) {
                response.put("userId", authResponse.getUserId());
            }
            if (authResponse.getEmail() != null) {
                response.put("email", authResponse.getEmail());
            }
            if (authResponse.getContactNumber() != null) {
                response.put("contactNumber", authResponse.getContactNumber());
            }
            if (authResponse.getDisplayName() != null) {
                response.put("displayName", authResponse.getDisplayName());
            }

            return ResponseEntity.ok(response);

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

        } catch (PhoneAlreadyLinkedException e) {

            response.put("success", false);
            response.put("code", "PHONE_ALREADY_LINKED");
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {

            e.printStackTrace();

            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
