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
    public ResponseEntity<Map<String, Object>> pushOrderToShiprocket(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String key) {
        if (!isAuthorized(key)) {
            log.warn("[INTERNAL:SHIPROCKET] push FORBIDDEN orderId={} (internal key mismatch or blank)", orderId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(
                    "Forbidden: internal service key mismatch. Set the same INTERNAL_SERVICE_KEY on user and admin."
            ));
        }
        log.info("[INTERNAL:SHIPROCKET] push orderId={}", orderId);
        try {
            ShiprocketShipmentResult result = orderService.pushOrderToShiprocket(orderId);
            if (result == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(
                        "Shiprocket push returned empty result"
                ));
            }
            return ResponseEntity.ok(successBody("Shiprocket shipment created", result));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(safeMsg(e, "Order not found")));
        } catch (OrderException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(safeMsg(e, "Invalid order for Shiprocket")));
        } catch (Exception e) {
            log.error("[INTERNAL:SHIPROCKET] push FAILED orderId={}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBodyWithDetail(e));
        }
    }

    @PostMapping("/orders/{orderId}/sync")
    public ResponseEntity<Map<String, Object>> syncOrderFromShiprocket(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String key) {
        if (!isAuthorized(key)) {
            log.warn("[INTERNAL:SHIPROCKET] sync FORBIDDEN orderId={} (internal key mismatch or blank)", orderId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(
                    "Forbidden: internal service key mismatch. Set the same INTERNAL_SERVICE_KEY on user and admin."
            ));
        }
        log.info("[INTERNAL:SHIPROCKET] sync orderId={}", orderId);
        try {
            ShiprocketShipmentResult result = orderService.syncOrderToShiprocket(orderId);
            if (result == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(
                        "Shiprocket sync returned empty result"
                ));
            }
            return ResponseEntity.ok(successBody("Shiprocket shipment synced", result));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(safeMsg(e, "Order not found")));
        } catch (OrderException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(safeMsg(e, "Invalid order for Shiprocket sync")));
        } catch (Exception e) {
            log.error("[INTERNAL:SHIPROCKET] sync FAILED orderId={}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBodyWithDetail(e));
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
        body.put("message", message != null && !message.isBlank() ? message : "Shiprocket request failed");
        return body;
    }

    private static Map<String, Object> errorBodyWithDetail(Throwable e) {
        String detail = safeMsg(e, "unknown");
        Map<String, Object> body = errorBody(detail);
        body.put("shipping_error_detail", detail);
        body.put("error_type", e.getClass().getSimpleName());
        return body;
    }

    private static String safeMsg(Throwable e, String fallback) {
        Throwable cur = e;
        while (cur != null) {
            if (cur.getMessage() != null && !cur.getMessage().isBlank()
                    && !"null".equalsIgnoreCase(cur.getMessage().trim())) {
                return cur.getMessage();
            }
            cur = cur.getCause();
        }
        return e != null ? e.getClass().getSimpleName() : fallback;
    }
}
