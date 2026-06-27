package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.*;
import com.ecommerce.authdemo.service.FaqCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faq-categories")
@RequiredArgsConstructor
public class FaqCategoryController {

    private final FaqCategoryService faqCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FaqCategoryResponse>>> getAll(
            @RequestParam(required = false) Boolean status) {
        List<FaqCategoryResponse> data = faqCategoryService.getAll(status);
        return ResponseEntity.ok(new ApiResponse<>(true, "FAQ categories fetched successfully", data));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FaqCategoryResponse>> create(
            @Valid @RequestBody FaqCategoryRequest request) {
        FaqCategoryResponse data = faqCategoryService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "FAQ category created successfully", data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FaqCategoryResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody FaqCategoryRequest request) {
        FaqCategoryResponse data = faqCategoryService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "FAQ category updated successfully", data));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FaqCategoryResponse>> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody FaqCategoryStatusUpdateRequest request) {
        FaqCategoryResponse data = faqCategoryService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(new ApiResponse<>(true, "FAQ category status updated successfully", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Integer id) {
        faqCategoryService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "FAQ category deleted successfully", "OK"));
    }
}
