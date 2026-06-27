package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ApiResponse;
import com.ecommerce.authdemo.dto.TicketResponseRequest;
import com.ecommerce.authdemo.dto.TicketResponseResponse;
import com.ecommerce.authdemo.service.TicketResponseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support-tickets/{ticketId}/responses")
@RequiredArgsConstructor
public class TicketResponseController {

    private final TicketResponseService ticketResponseService;

    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponseResponse>> create(
            @PathVariable Integer ticketId,
            @Valid @RequestBody TicketResponseRequest request) {
        TicketResponseResponse data = ticketResponseService.create(ticketId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Ticket response created successfully", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketResponseResponse>>> getByTicketId(
            @PathVariable Integer ticketId) {
        List<TicketResponseResponse> data = ticketResponseService.getByTicketId(ticketId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Ticket responses fetched successfully", data));
    }
}
