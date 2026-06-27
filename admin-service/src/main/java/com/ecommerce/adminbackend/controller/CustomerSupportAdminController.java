package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.dto.common.NoteRequest;
import com.ecommerce.adminbackend.service.CustomerSupportAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/customer-support")
@RequiredArgsConstructor
public class CustomerSupportAdminController {

    private final CustomerSupportAdminService customerSupportAdminService;

    @GetMapping("/tickets/stats")
    public Map<String, Object> ticketStats() {
        return customerSupportAdminService.ticketStats();
    }

    @GetMapping("/tickets")
    public PageResponse<Map<String, Object>> listTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return customerSupportAdminService.listTickets(status, type, search, page, size);
    }

    @GetMapping("/tickets/{id}")
    public Map<String, Object> getTicket(@PathVariable Integer id) {
        return customerSupportAdminService.getTicket(id);
    }

    @PostMapping("/tickets/{id}/reply")
    public Map<String, Object> reply(@PathVariable Integer id, @RequestBody NoteRequest request) {
        String message = request != null ? firstNonBlank(request.getReply(), request.getMessage()) : null;
        return customerSupportAdminService.addResponse(id, message);
    }

    @PatchMapping("/tickets/{id}/status")
    public Map<String, Object> updateStatus(@PathVariable Integer id, @RequestBody NoteRequest request) {
        return customerSupportAdminService.updateStatus(id, request != null ? request.getStatus() : null);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }
}
