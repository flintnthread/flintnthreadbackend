package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.*;
import com.ecommerce.authdemo.service.ContactMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactMessageController {

    private final ContactMessageService contactMessageService;

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> submit(
            @Valid @RequestBody ContactMessageRequest request) {
        ContactMessageResponse response = contactMessageService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Message submitted successfully", response));
    }

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<ContactMessageResponse>>> getMessages(
            @RequestParam(required = false) Boolean status) {
        List<ContactMessageResponse> data = contactMessageService.getAll(status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Messages fetched successfully", data));
    }

    @PatchMapping("/messages/{id}/status")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody ContactStatusUpdateRequest request) {
        ContactMessageResponse data = contactMessageService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(new ApiResponse<>(true, "Message status updated successfully", data));
    }
}
