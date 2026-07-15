package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.service.HomepageSectionAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/cms/homepage-sections")
@RequiredArgsConstructor
public class HomepageSectionAdminController {

    private final HomepageSectionAdminService service;

    @GetMapping
    public List<Map<String, Object>> list() {
        return service.list();
    }

    @PutMapping
    public List<Map<String, Object>> upsert(@RequestBody List<Map<String, Object>> items) {
        return service.upsert(items);
    }
}
