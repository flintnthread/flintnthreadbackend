package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.repository.OrderRepository;
import com.ecommerce.authdemo.service.ShiprocketService;
import com.ecommerce.authdemo.service.ShiprocketWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shiprocket")
@RequiredArgsConstructor
@CrossOrigin("*")
@Slf4j
public class ShiprocketWebhookController {

    private final ShiprocketService shiprocketService;
    private final ShiprocketWebhookService shiprocketWebhookService;
    private final OrderRepository orderRepository;

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> receiveWebhook(
            @RequestBody(required = false) Map<String, Object> payload) {
        log.info("[SHIPROCKET:WEBHOOK] HTTP POST /api/shiprocket/webhook received payloadSize={}",
                payload == null ? 0 : payload.size());
        try {
            if (payload == null) {
                log.warn("[SHIPROCKET:WEBHOOK] empty body");
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Empty webhook payload"
                ));
            }

            // Single handler to avoid conflicting/double status transitions.
            shiprocketWebhookService.handleWebhook(payload);

            Map<String, Object> ok = new HashMap<>();
            ok.put("success", true);
            ok.put("message", "Webhook processed successfully");
            ok.put("processed", true);
            log.info("[SHIPROCKET:WEBHOOK] HTTP 200 processed=true");
            return ResponseEntity.ok(ok);

        } catch (Exception e) {
            log.error("[SHIPROCKET:WEBHOOK] HTTP 500 {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to process webhook",
                    "processed", false
            ));
        }
    }

    @GetMapping("/track/{awb}")
    public ResponseEntity<Map<String, Object>> trackShipment(@PathVariable String awb) {
        log.info("[SHIPROCKET:API] HTTP GET /api/shiprocket/track/{}", awb);
        try {
            String trackingInfo = shiprocketService.trackShipment(awb);

            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("message", "Tracking information retrieved");
            body.put("data", trackingInfo);
            log.info("[SHIPROCKET:API] track OK awb={} dataLength={}", awb, trackingInfo != null ? trackingInfo.length() : 0);
            return ResponseEntity.ok(body);

        } catch (Exception e) {
            log.error("[SHIPROCKET:API] track FAILED awb={} msg={}", awb, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to track shipment"
            ));
        }
    }

    /**
     * Pull AWB + tracking URL from Shiprocket into local {@code orders} row
     * (fixes Ship Now done in Shiprocket UI when webhook did not update DB).
     */
    @PostMapping("/sync-order/{orderId}")
    public ResponseEntity<Map<String, Object>> syncOrderShipment(@PathVariable Long orderId) {
        log.info("[SHIPROCKET:API] HTTP POST /api/shiprocket/sync-order/{}", orderId);
        try {
            if (orderId == null || orderId <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Valid order ID is required"
                ));
            }
            var order = orderRepository.findById(orderId)
                    .orElse(null);
            if (order == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "Order not found"
                ));
            }

            var result = shiprocketService.syncShipmentDetails(order);
            Map<String, Object> data = new HashMap<>();
            data.put("orderId", order.getId());
            data.put("orderNumber", order.getOrderNumber());
            data.put("shiprocketOrderId", order.getShiprocketOrderId());
            data.put("shiprocketShipmentId", order.getShiprocketShipmentId());
            data.put("shiprocketAwbCode", result != null ? result.getAwbCode() : order.getShiprocketAwbCode());
            data.put("shiprocketTrackingUrl",
                    result != null ? result.getTrackingUrl() : order.getShiprocketTrackingUrl());
            data.put("shiprocketCourierName",
                    result != null ? result.getCourierName() : order.getShiprocketCourierName());
            data.put("orderStatus", order.getOrderStatus());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Shiprocket shipment synced",
                    "data", data
            ));
        } catch (Exception e) {
            log.error("[SHIPROCKET:API] sync-order FAILED orderId={} msg={}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to sync shipment"
            ));
        }
    }

    /**
     * Manual catch-up for recent active Shiprocket orders when webhook updates were missed.
     * Example: POST /api/shiprocket/sync-active?limit=200&lookbackHours=168
     */
    @PostMapping("/sync-active")
    public ResponseEntity<Map<String, Object>> syncRecentActiveOrders(
            @RequestParam(defaultValue = "200") int limit,
            @RequestParam(defaultValue = "168") int lookbackHours,
            @RequestParam(defaultValue = "30") int minSyncAgeMinutes
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        int safeLookback = Math.max(1, Math.min(lookbackHours, 24 * 30));
        int safeMinAge = Math.max(1, Math.min(minSyncAgeMinutes, 24 * 60));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAfter = now.minusHours(safeLookback);
        LocalDateTime syncedBefore = now.minusMinutes(safeMinAge);
        log.info("[SHIPROCKET:API] HTTP POST /api/shiprocket/sync-active limit={} lookbackHours={} minSyncAgeMinutes={}",
                safeLimit, safeLookback, safeMinAge);
        try {
            List<com.ecommerce.authdemo.entity.Order> candidates =
                    orderRepository.findShiprocketStatusSyncCandidates(
                            createdAfter,
                            syncedBefore,
                            PageRequest.of(0, safeLimit)
                    );

            int success = 0;
            int failed = 0;
            for (com.ecommerce.authdemo.entity.Order order : candidates) {
                try {
                    shiprocketService.syncShipmentDetails(order);
                    success++;
                } catch (Exception ex) {
                    failed++;
                    log.warn("[SHIPROCKET:API] sync-active orderId={} orderNumber={} failed={}",
                            order.getId(), order.getOrderNumber(), ex.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Shiprocket catch-up sync completed",
                    "data", Map.of(
                            "requestedLimit", safeLimit,
                            "candidates", candidates.size(),
                            "synced", success,
                            "failed", failed,
                            "lookbackHours", safeLookback,
                            "minSyncAgeMinutes", safeMinAge
                    )
            ));
        } catch (Exception e) {
            log.error("[SHIPROCKET:API] sync-active FAILED msg={}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to run catch-up sync"
            ));
        }
    }
}
