package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.service.AdsCustomerAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/ads/customers")
@RequiredArgsConstructor
public class AdsCustomerAdminController {

    private final AdsCustomerAdminService service;

    @GetMapping
    public PageResponse<Map<String, Object>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(search, page, size);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Integer id) {
        return service.get(id);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Integer id) {
        return service.delete(id);
    }
}
