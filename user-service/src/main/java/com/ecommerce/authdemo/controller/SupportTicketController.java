package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ApiResponse;
import com.ecommerce.authdemo.dto.SupportTicketEditRequest;
import com.ecommerce.authdemo.dto.SupportTicketRequest;
import com.ecommerce.authdemo.dto.SupportTicketResponse;
import com.ecommerce.authdemo.dto.SupportTicketStatusUpdateRequest;
import com.ecommerce.authdemo.service.SupportTicketService;
import com.ecommerce.authdemo.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support-tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService supportTicketService;
    private final SecurityUtil securityUtil;

    @PostMapping
    public ResponseEntity<ApiResponse<SupportTicketResponse>> create(
            @Valid @RequestBody SupportTicketRequest request) {
        SupportTicketResponse data = supportTicketService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Support ticket created successfully", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupportTicketResponse>>> getTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        Integer customerId = Math.toIntExact(securityUtil.getCurrentUserId());
        List<SupportTicketResponse> data = supportTicketService.getTickets(customerId, status, type);
        return ResponseEntity.ok(new ApiResponse<>(true, "Support tickets fetched successfully", data));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody SupportTicketStatusUpdateRequest request) {
        SupportTicketResponse data = supportTicketService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(new ApiResponse<>(true, "Support ticket status updated successfully", data));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> editMyTicket(
            @PathVariable Integer id,
            @Valid @RequestBody SupportTicketEditRequest request) {
        SupportTicketResponse data = supportTicketService.editByCustomer(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Support ticket updated successfully", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> delete(@PathVariable Integer id) {
        SupportTicketResponse data = supportTicketService.deleteByCustomer(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Support ticket deleted successfully", data));
    }
}
