package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.support.*;
import com.ecommerce.sellerbackend.entity.SellerSupportFeedback;
import com.ecommerce.sellerbackend.repository.SellerSupportFeedbackRepository;
import com.ecommerce.sellerbackend.service.SupportFileStorageService;
import com.ecommerce.sellerbackend.service.SupportTicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/support")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;
    private final SupportFileStorageService fileStorageService;
    private final SellerSupportFeedbackRepository sellerSupportFeedbackRepository;

    // ── Upload ──────────────────────────────────────────────────────────────

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) throws IOException {
        String attachment = fileStorageService.store(file, request);
        return ResponseEntity.ok(UploadResponse.builder().attachment(attachment).build());
    }

    // ── Ticket Endpoints ────────────────────────────────────────────────────

    @PostMapping("/tickets")
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse ticket = supportTicketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    @PostMapping(value = "/tickets/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketResponse> createTicketWithFile(
            @RequestParam Long sellerId,
            @RequestParam String subject,
            @RequestParam String category,
            @RequestParam String priority,
            @RequestParam(required = false) String description,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) throws IOException {
        String attachmentUrl = fileStorageService.store(file, request);
        CreateTicketRequest ticketRequest = CreateTicketRequest.builder()
                .sellerId(sellerId)
                .subject(subject)
                .category(category)
                .priority(priority)
                .description(description)
                .build();
        TicketResponse ticket = supportTicketService.createTicketWithAttachment(ticketRequest, attachmentUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    @GetMapping("/tickets/seller/{sellerId}")
    public ResponseEntity<List<TicketResponse>> getTicketsBySeller(@PathVariable Long sellerId) {
        return ResponseEntity.ok(supportTicketService.getTicketsBySeller(sellerId));
    }

    @GetMapping("/tickets/seller/{sellerId}/status/{status}")
    public ResponseEntity<List<TicketResponse>> getTicketsByStatus(
            @PathVariable Long sellerId,
            @PathVariable String status) {
        return ResponseEntity.ok(supportTicketService.getTicketsBySellerAndStatus(sellerId, status));
    }

    @GetMapping("/tickets/{ticketId}/seller/{sellerId}")
    public ResponseEntity<TicketResponse> getTicketById(
            @PathVariable Long ticketId,
            @PathVariable Long sellerId) {
        return ResponseEntity.ok(supportTicketService.getTicketById(ticketId, sellerId));
    }

    @GetMapping("/tickets/number/{ticketNumber}")
    public ResponseEntity<TicketResponse> getTicketByNumber(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(supportTicketService.getTicketByNumber(ticketNumber));
    }

    @PatchMapping("/tickets/{ticketId}/close")
    public ResponseEntity<TicketResponse> closeTicket(
            @PathVariable Long ticketId,
            @RequestParam Long sellerId) {
        return ResponseEntity.ok(supportTicketService.closeTicket(ticketId, sellerId));
    }

    // ── Message Endpoints ───────────────────────────────────────────────────

    @PostMapping("/messages")
    public ResponseEntity<MessageResponse> addMessage(@Valid @RequestBody CreateMessageRequest request) {
        MessageResponse message = supportTicketService.addMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @PostMapping(value = "/messages/with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> addMessageWithFile(
            @RequestParam Long ticketId,
            @RequestParam String senderType,
            @RequestParam Long senderId,
            @RequestParam(required = false) String message,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) throws IOException {
        String attachmentUrl = fileStorageService.store(file, request);
        CreateMessageRequest msgRequest = CreateMessageRequest.builder()
                .ticketId(ticketId)
                .senderType(senderType)
                .senderId(senderId)
                .message(message)
                .attachment(attachmentUrl)
                .build();
        MessageResponse response = supportTicketService.addMessage(msgRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/messages/ticket/{ticketId}")
    public ResponseEntity<List<MessageResponse>> getMessages(@PathVariable Long ticketId) {
        return ResponseEntity.ok(supportTicketService.getMessagesByTicket(ticketId));
    }

    // ── Support feedback (Rate your experience) ─────────────────────────
    @PostMapping("/feedback")
    public ResponseEntity<Map<String, String>> submitFeedback(@Valid @RequestBody SubmitFeedbackRequest request) {
        SellerSupportFeedback feedback = SellerSupportFeedback.builder()
                .sellerId(request.getSellerId())
                .rating(request.getRating())
                .feedbackText(request.getFeedbackText())
                .build();

        sellerSupportFeedbackRepository.save(feedback);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Thanks! Your feedback has been submitted."));
    }
}
