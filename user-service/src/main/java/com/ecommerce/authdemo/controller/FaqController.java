package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.*;
import com.ecommerce.authdemo.service.FaqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FaqResponse>>> getAll(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean status) {
        List<FaqResponse> data = faqService.getAll(categoryId, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "FAQs fetched successfully", data));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FaqResponse>> create(
            @Valid @RequestBody FaqRequest request) {
        FaqResponse data = faqService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "FAQ created successfully", data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FaqResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody FaqRequest request) {
        FaqResponse data = faqService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "FAQ updated successfully", data));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FaqResponse>> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody FaqStatusUpdateRequest request) {
        FaqResponse data = faqService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(new ApiResponse<>(true, "FAQ status updated successfully", data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Integer id) {
        faqService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "FAQ deleted successfully", "OK"));
    }
}
