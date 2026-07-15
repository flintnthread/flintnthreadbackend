package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.service.SiteLogoAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/cms/logos")
@RequiredArgsConstructor
public class SiteLogoAdminController {

    private final SiteLogoAdminService service;

    @GetMapping
    public Map<String, Object> get() {
        return service.get();
    }

    @PutMapping
    public Map<String, Object> update(@RequestBody Map<String, Object> body) {
        return service.update(body);
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(
            @RequestParam String slot,
            @RequestParam("file") MultipartFile file) {
        return service.upload(slot, file);
    }
}
