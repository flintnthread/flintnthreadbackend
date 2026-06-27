package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.service.ShiprocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/shiprocket")
@RequiredArgsConstructor
@CrossOrigin("*")
@Slf4j
public class ShiprocketWebhookController {

    private final ShiprocketService shiprocketService;

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

            shiprocketService.handleWebhook(payload);

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
}
