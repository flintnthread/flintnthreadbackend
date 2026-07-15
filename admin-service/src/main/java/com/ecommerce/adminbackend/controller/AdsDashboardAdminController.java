package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.service.AdsDashboardAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/ads/dashboard")
@RequiredArgsConstructor
public class AdsDashboardAdminController {

    private final AdsDashboardAdminService service;

    @GetMapping
    public Map<String, Object> dashboard(@RequestParam(required = false, defaultValue = "monthly") String period) {
        return service.dashboard(period);
    }
}
