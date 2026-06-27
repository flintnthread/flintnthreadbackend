package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.*;
import com.ecommerce.authdemo.service.DeliveryChargesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/delivery-charges")
@RequiredArgsConstructor
public class DeliveryChargesController {

    private final DeliveryChargesService deliveryChargesService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliveryChargeResponse>>> getAll(
            @RequestParam(required = false) Boolean status) {
        List<DeliveryChargeResponse> data = deliveryChargesService.getAll(status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Delivery charges fetched successfully", data));
    }

    @GetMapping("/by-weight")
    public ResponseEntity<ApiResponse<DeliveryChargeResponse>> getByWeight(
            @RequestParam BigDecimal weight) {
        DeliveryChargeResponse data = deliveryChargesService.getByWeight(weight);
        return ResponseEntity.ok(new ApiResponse<>(true, "Delivery charge slab fetched successfully", data));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryChargeResponse>> create(
            @Valid @RequestBody DeliveryChargeRequest request) {
        DeliveryChargeResponse data = deliveryChargesService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Delivery charge slab created successfully", data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryChargeResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody DeliveryChargeRequest request) {
        DeliveryChargeResponse data = deliveryChargesService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Delivery charge slab updated successfully", data));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DeliveryChargeResponse>> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody DeliveryChargeStatusUpdateRequest request) {
        DeliveryChargeResponse data = deliveryChargesService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(new ApiResponse<>(true, "Delivery charge slab status updated successfully", data));
    }
}
