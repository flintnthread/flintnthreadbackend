package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import com.ecommerce.adminbackend.dto.settings.CommissionRequest;
import com.ecommerce.adminbackend.service.SettingsAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class SettingsAdminController {

    private static final Logger log = LogFactory.getLogger(SettingsAdminController.class);

    private final SettingsAdminService settingsAdminService;

    @GetMapping("/commission")
    public Map<String, String> getCommission() {
        return settingsAdminService.getCommission();
    }

    @PutMapping("/commission")
    public Map<String, String> updateCommission(@RequestBody CommissionRequest request) {
        return settingsAdminService.updateCommission(
                request != null ? request.getB2c() : null,
                request != null ? request.getB2b() : null);
    }
}
