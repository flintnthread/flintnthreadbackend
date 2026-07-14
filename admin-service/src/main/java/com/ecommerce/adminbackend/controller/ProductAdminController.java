package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.dto.common.NoteRequest;
import com.ecommerce.adminbackend.dto.product.CreateProductRequest;
import com.ecommerce.adminbackend.dto.product.UpdateProductRequest;
import com.ecommerce.adminbackend.logging.LogFactory;
import com.ecommerce.adminbackend.service.ProductAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody CreateProductRequest request) {
        log.info("Admin creating product name={}", request.getName());
        return productAdminService.create(request);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getProduct(@PathVariable Long id) {
        return productAdminService.getProduct(id);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        log.info("Admin updating product id={}", id);
        return productAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        log.info("Admin deleting product id={}", id);
        productAdminService.delete(id);
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
