package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.service.HomepageBannerAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/cms/homepage-banners")
@RequiredArgsConstructor
public class HomepageBannerAdminController {

    private final HomepageBannerAdminService service;

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String section) {
        return service.list(section);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Integer id) {
        return service.get(id);
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        return service.create(body);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Integer id) {
        service.delete(id);
        return Map.of("message", "Homepage banner deleted.");
    }
}
