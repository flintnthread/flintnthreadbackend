package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/health")
public class HealthController {

    private static final Logger log = LogFactory.getLogger(HealthController.class);

    @GetMapping
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "admin-backend");
    }
}
