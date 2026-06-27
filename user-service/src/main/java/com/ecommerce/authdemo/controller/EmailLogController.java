package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ApiResponse;
import com.ecommerce.authdemo.dto.EmailLogResponse;
import com.ecommerce.authdemo.entity.EmailLogStatus;
import com.ecommerce.authdemo.service.EmailLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/email-logs")
@RequiredArgsConstructor
public class EmailLogController {

    private final EmailLogService emailLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmailLogResponse>>> getLogs(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String emailType,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) EmailLogStatus status) {
        List<EmailLogResponse> data = emailLogService.getLogs(userId, emailType, recipient, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Email logs fetched successfully", data));
    }
}
