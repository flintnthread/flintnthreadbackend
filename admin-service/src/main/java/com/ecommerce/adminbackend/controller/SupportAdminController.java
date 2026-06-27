package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.dto.common.NoteRequest;
import com.ecommerce.adminbackend.service.SupportAdminService;
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
@RequestMapping("/api/admin/support")
@RequiredArgsConstructor
public class SupportAdminController {

    private static final Logger log = LogFactory.getLogger(SupportAdminController.class);

    private final SupportAdminService supportAdminService;

    @GetMapping("/tickets/stats")
    public Map<String, Object> ticketStats() {
        return supportAdminService.ticketStats();
    }

    @GetMapping("/tickets")
    public PageResponse<Map<String, Object>> listTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return supportAdminService.listTickets(status, priority, search, page, size);
    }

    @GetMapping("/tickets/{id}")
    public Map<String, Object> getTicket(@PathVariable Long id) {
        return supportAdminService.getTicket(id);
    }

    @PostMapping("/tickets/{id}/messages")
    public Map<String, Object> addMessage(@PathVariable Long id, @RequestBody NoteRequest request) {
        return supportAdminService.addMessage(id, request != null ? request.getMessage() : null);
    }

    @PatchMapping("/tickets/{id}/status")
    public Map<String, Object> updateStatus(@PathVariable Long id, @RequestBody NoteRequest request) {
        return supportAdminService.updateStatus(id, request != null ? request.getStatus() : null);
    }
}
