package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.service.AdsPaymentAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/ads/payments")
@RequiredArgsConstructor
public class AdsPaymentAdminController {

    private final AdsPaymentAdminService service;

    @GetMapping
    public PageResponse<Map<String, Object>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(search, status, page, size);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return service.stats();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Integer id) {
        return service.get(id);
    }
}
