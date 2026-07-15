package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.service.AdminEmailBroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/emails")
@RequiredArgsConstructor
public class AdminEmailBroadcastController {

    private final AdminEmailBroadcastService service;

    @PostMapping("/customers")
    public Map<String, Object> sendToCustomers(@RequestBody Map<String, Object> body) {
        return service.sendToCustomers(body);
    }

    @PostMapping("/sellers")
    public Map<String, Object> sendToSellers(@RequestBody Map<String, Object> body) {
        return service.sendToSellers(body);
    }
}
