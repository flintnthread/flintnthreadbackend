package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.dto.common.NoteRequest;
import com.ecommerce.adminbackend.service.CategoryRequestAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/category-requests")
@RequiredArgsConstructor
public class CategoryRequestAdminController {

    private static final Logger log = LogFactory.getLogger(CategoryRequestAdminController.class);

    private final CategoryRequestAdminService categoryRequestAdminService;

    @GetMapping
    public PageResponse<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return categoryRequestAdminService.listRequests(status, page, size);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return categoryRequestAdminService.stats();
    }

    @PostMapping("/{id}/approve")
    public Map<String, Object> approve(@PathVariable Long id, @RequestBody(required = false) NoteRequest request) {
        return categoryRequestAdminService.approve(id, request != null ? request.getNote() : null);
    }

    @PostMapping("/{id}/reject")
    public Map<String, Object> reject(@PathVariable Long id, @RequestBody(required = false) NoteRequest request) {
        String note = request != null ? (request.getNote() != null ? request.getNote() : request.getReason()) : null;
        return categoryRequestAdminService.reject(id, note);
    }
}
