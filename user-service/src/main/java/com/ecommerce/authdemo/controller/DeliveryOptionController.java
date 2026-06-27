package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.*;
import com.ecommerce.authdemo.service.DeliveryOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery-options")
@RequiredArgsConstructor
public class DeliveryOptionController {

    private final DeliveryOptionService deliveryOptionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliveryOptionResponse>>> getAll(
            @RequestParam(required = false) Integer sellerId,
            @RequestParam(required = false) Boolean isActive) {
        List<DeliveryOptionResponse> data = deliveryOptionService.getAll(sellerId, isActive);
        return ResponseEntity.ok(new ApiResponse<>(true, "Delivery options fetched successfully", data));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryOptionResponse>> create(
            @Valid @RequestBody DeliveryOptionRequest request) {
        DeliveryOptionResponse data = deliveryOptionService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Delivery option created successfully", data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryOptionResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody DeliveryOptionRequest request) {
        DeliveryOptionResponse data = deliveryOptionService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Delivery option updated successfully", data));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DeliveryOptionResponse>> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody DeliveryOptionStatusUpdateRequest request) {
        DeliveryOptionResponse data = deliveryOptionService.updateStatus(id, request.getIsActive());
        return ResponseEntity.ok(new ApiResponse<>(true, "Delivery option status updated successfully", data));
    }
}
