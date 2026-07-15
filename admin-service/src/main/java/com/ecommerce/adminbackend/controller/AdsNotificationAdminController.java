package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.service.AdsNotificationAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/ads/notifications")
@RequiredArgsConstructor
public class AdsNotificationAdminController {

    private final AdsNotificationAdminService service;

    @GetMapping
    public PageResponse<Map<String, Object>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(search, status, unreadOnly, page, size);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return service.stats();
    }

    @PatchMapping("/{id}")
    public Map<String, Object> patch(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        return service.patch(id, body);
    }
}
