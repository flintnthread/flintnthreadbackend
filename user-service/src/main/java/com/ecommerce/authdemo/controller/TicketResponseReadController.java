package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ApiResponse;
import com.ecommerce.authdemo.dto.TicketResponseReadRequest;
import com.ecommerce.authdemo.dto.TicketResponseReadResponse;
import com.ecommerce.authdemo.service.TicketResponseReadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ticket-response-reads")
@RequiredArgsConstructor
public class TicketResponseReadController {

    private final TicketResponseReadService ticketResponseReadService;

    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponseReadResponse>> markAsRead(
            @Valid @RequestBody TicketResponseReadRequest request) {
        TicketResponseReadResponse data = ticketResponseReadService.markAsRead(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Ticket response marked as read", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketResponseReadResponse>>> getReads(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Integer responseId) {
        List<TicketResponseReadResponse> data;
        if (userId != null) {
            data = ticketResponseReadService.getByUserId(userId);
        } else if (responseId != null) {
            data = ticketResponseReadService.getByResponseId(responseId);
        } else {
            data = List.of();
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Ticket response reads fetched successfully", data));
    }
}
