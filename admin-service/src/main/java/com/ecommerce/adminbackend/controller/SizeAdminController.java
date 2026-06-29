package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.service.SizeAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sizes")
@RequiredArgsConstructor
public class SizeAdminController {

    private final SizeAdminService sizeAdminService;

    @GetMapping
    public List<Map<String, Object>> list() {
        return sizeAdminService.list();
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> request) {
        return sizeAdminService.create(request);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return sizeAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        sizeAdminService.delete(id);
    }
}
