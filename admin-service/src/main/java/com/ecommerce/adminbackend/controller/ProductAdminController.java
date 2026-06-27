package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.dto.common.NoteRequest;
import com.ecommerce.adminbackend.service.ProductAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {

    private static final Logger log = LogFactory.getLogger(ProductAdminController.class);

    private final ProductAdminService productAdminService;

    @GetMapping
    public PageResponse<Map<String, Object>> listProducts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Boolean adminOnly,
            @RequestParam(required = false) Integer mainCategoryId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer subcategoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productAdminService.listProducts(
                status, search, sellerId, adminOnly, mainCategoryId, categoryId, subcategoryId, page, size);
    }

    @GetMapping("/pending")
    public PageResponse<Map<String, Object>> pending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productAdminService.listPending(page, size);
    }

    @GetMapping("/catalog")
    public Map<String, Object> catalog() {
        return productAdminService.catalog();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return productAdminService.stats();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getProduct(@PathVariable Long id) {
        return productAdminService.getProduct(id);
    }

    @PostMapping("/{id}/approve")
    public Map<String, Object> approve(@PathVariable Long id, @RequestBody(required = false) NoteRequest request) {
        return productAdminService.approve(id, request != null ? request.getNote() : null);
    }

    @PostMapping("/{id}/reject")
    public Map<String, Object> reject(@PathVariable Long id, @RequestBody(required = false) NoteRequest request) {
        String note = request != null ? (request.getNote() != null ? request.getNote() : request.getReason()) : null;
        return productAdminService.reject(id, note);
    }
}
