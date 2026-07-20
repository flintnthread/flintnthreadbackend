package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ShiprocketShipmentResult;
import com.ecommerce.authdemo.exception.OrderException;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Service-to-service Shiprocket push (seller-service calls after seller confirms order).
 */
@RestController
@RequestMapping("/api/internal/shiprocket")
@RequiredArgsConstructor
@Slf4j
public class InternalShiprocketController {

    private final OrderService orderService;

    @Value("${app.internal-service-key:}")
    private String internalServiceKey;

    @PostMapping("/orders/{orderId}/push")
    public ResponseEntity<?> pushOrderToShiprocket(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String key) {
        if (internalServiceKey == null
                || internalServiceKey.isBlank()
                || !internalServiceKey.equals(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "Forbidden"
            ));
        }
        log.info("[INTERNAL:SHIPROCKET] push orderId={}", orderId);
        try {
            ShiprocketShipmentResult result = orderService.pushOrderToShiprocket(orderId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Shiprocket shipment created",
                    "shipping_initiated", true,
                    "shiprocket", result.toMap()
            ));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (OrderException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("[INTERNAL:SHIPROCKET] push FAILED orderId={}", orderId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Shiprocket push failed",
                    "shipping_error_detail", e.getMessage() != null ? e.getMessage() : "unknown"
            ));
        }
    }
}
