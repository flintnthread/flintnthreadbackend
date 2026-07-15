package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.service.CatalogImageStorageService;
import com.ecommerce.adminbackend.service.GeneralBannerAdminService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/cms/general-banners")
@RequiredArgsConstructor
public class GeneralBannerAdminController {

    private final GeneralBannerAdminService service;
    private final CatalogImageStorageService catalogImageStorageService;

    @GetMapping
    public List<Map<String, Object>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer status) {
        return service.list(search, status);
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
        return Map.of("message", "Banner deleted.");
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        String path = catalogImageStorageService.storeCmsMedia(file, "banners");
        return Map.of("path", path, "message", "Image uploaded.");
    }
}
