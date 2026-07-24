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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service-to-service Shiprocket push/sync (seller-service / admin-service).
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
        if (!isAuthorized(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "Forbidden"
            ));
        }
        log.info("[INTERNAL:SHIPROCKET] push orderId={}", orderId);
        try {
            ShiprocketShipmentResult result = orderService.pushOrderToShiprocket(orderId);
            return ResponseEntity.ok(successBody("Shiprocket shipment created", result));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(errorBody(e.getMessage()));
        } catch (OrderException e) {
            return ResponseEntity.badRequest().body(errorBody(e.getMessage()));
        } catch (Exception e) {
            log.error("[INTERNAL:SHIPROCKET] push FAILED orderId={}", orderId, e);
            String detail = e.getMessage() != null ? e.getMessage() : "unknown";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", detail,
                    "shipping_error_detail", detail
            ));
        }
    }

    @PostMapping("/orders/{orderId}/sync")
    public ResponseEntity<?> syncOrderFromShiprocket(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String key) {
        if (!isAuthorized(key)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false,
                    "message", "Forbidden"
            ));
        }
        log.info("[INTERNAL:SHIPROCKET] sync orderId={}", orderId);
        try {
            ShiprocketShipmentResult result = orderService.syncOrderToShiprocket(orderId);
            return ResponseEntity.ok(successBody("Shiprocket shipment synced", result));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(errorBody(e.getMessage()));
        } catch (OrderException e) {
            return ResponseEntity.badRequest().body(errorBody(e.getMessage()));
        } catch (Exception e) {
            log.error("[INTERNAL:SHIPROCKET] sync FAILED orderId={}", orderId, e);
            String detail = e.getMessage() != null ? e.getMessage() : "unknown";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", detail,
                    "shipping_error_detail", detail
            ));
        }
    }

    private boolean isAuthorized(String key) {
        return internalServiceKey != null
                && !internalServiceKey.isBlank()
                && internalServiceKey.equals(key);
    }

    private static Map<String, Object> successBody(String message, ShiprocketShipmentResult result) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", message);
        body.put("shipping_initiated", true);
        body.put("shiprocket", result.toMap());
        return body;
    }

    private static Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message != null ? message : "Shiprocket request failed");
        return body;
    }
}
