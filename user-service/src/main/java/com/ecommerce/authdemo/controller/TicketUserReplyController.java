package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ApiResponse;
import com.ecommerce.authdemo.dto.TicketUserReplyRequest;
import com.ecommerce.authdemo.dto.TicketUserReplyResponse;
import com.ecommerce.authdemo.service.TicketUserReplyService;
import com.ecommerce.authdemo.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/support-tickets/{ticketId}/user-replies")
@RequiredArgsConstructor
public class TicketUserReplyController {

    private final TicketUserReplyService ticketUserReplyService;
    private final SecurityUtil securityUtil;

    @PostMapping
    public ResponseEntity<ApiResponse<TicketUserReplyResponse>> create(
            @PathVariable Integer ticketId,
            @Valid @RequestBody TicketUserReplyRequest request) {
        Integer resolvedUserId = securityUtil.tryGetCurrentUserId()
                .map(Math::toIntExact)
                .orElseGet(() -> request.getUserId());
        if (resolvedUserId == null || resolvedUserId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User ID is required. Please sign in again."
            );
        }
        request.setUserId(resolvedUserId);
        TicketUserReplyResponse data = ticketUserReplyService.create(ticketId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Ticket user reply created successfully", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketUserReplyResponse>>> getByTicketId(
            @PathVariable Integer ticketId) {
        List<TicketUserReplyResponse> data = ticketUserReplyService.getByTicketId(ticketId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Ticket user replies fetched successfully", data));
    }
}
