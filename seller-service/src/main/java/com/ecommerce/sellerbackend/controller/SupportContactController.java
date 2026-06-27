package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.support.*;
import com.ecommerce.sellerbackend.service.SupportContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/support/contact")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupportContactController {

    private final SupportContactService supportContactService;

    /** Chat, email, call configuration for Contact Support section */
    @GetMapping
    public ResponseEntity<SupportContactConfigResponse> getContactConfig() {
        return ResponseEntity.ok(supportContactService.getContactConfig());
    }

    /** Save email support inquiry (contacts table) */
    @PostMapping("/email")
    public ResponseEntity<?> sendEmail(@Valid @RequestBody SendSupportEmailRequest request) {
        try {
            supportContactService.sendEmailInquiry(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Your email has been sent. We will reply soon."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to send email. Please try again."));
        }
    }

    /** Live chat history for seller */
    @GetMapping("/chat/{sellerId}")
    public ResponseEntity<List<LiveChatMessageResponse>> getChatHistory(@PathVariable Integer sellerId) {
        return ResponseEntity.ok(supportContactService.getChatHistory(sellerId));
    }

    /** Send live chat message + receive bot reply */
    @PostMapping("/chat")
    public ResponseEntity<List<LiveChatMessageResponse>> sendChatMessage(
            @Valid @RequestBody LiveChatMessageRequest request) {
        return ResponseEntity.ok(supportContactService.sendChatMessage(request));
    }
}
