package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ShiprocketShipmentResult;
import com.ecommerce.authdemo.entity.Order;
import com.ecommerce.authdemo.service.OrderService;
import com.ecommerce.authdemo.service.RazorpayService;
import com.ecommerce.authdemo.service.ShiprocketService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final RazorpayService razorpayService;
    private final OrderService orderService;
    private final ShiprocketService shiprocketService;

    private static Double resolveAmount(Double queryAmount, Map<String, Object> body) {
        if (queryAmount != null) {
            return queryAmount;
        }
        if (body == null || !body.containsKey("amount")) {
            return null;
        }
        Object raw = body.get("amount");
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number n) {
            return n.doubleValue();
        }
        if (raw instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @GetMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrderGetNotAllowed() {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", "Use HTTP POST, not GET. Opening this URL in a browser always sends GET.");
        body.put("method", "POST");
        body.put("urlExample", "/api/payment/create-order?amount=1");
        body.put("bodyExample", "{\"amount\":1}");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPaymentGetNotAllowed() {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", "Use HTTP POST, not GET.");
        body.put("method", "POST");
        body.put("urlExample", "/api/payment/verify?orderId=...&paymentId=...&signature=...");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @RequestParam(required = false) Double amount,
            @RequestBody(required = false) Map<String, Object> body) {

        Double resolved = resolveAmount(amount, body);
        if (resolved == null || resolved <= 0) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Missing or invalid amount");
            err.put("hint", "Use query only in the URL bar: http://localhost:8080/api/payment/create-order?amount=1 (do not type POST there). Or Body → raw JSON: {\"amount\":1}");
            return ResponseEntity.badRequest().body(err);
        }

        logger.info("[PAYMENT] create-order START amountInr={}", resolved);
        try {
            JSONObject order = razorpayService.createOrder(resolved);
            String razorpayOrderId = order.get("id").toString();
            orderService.linkRazorpayOrder(razorpayOrderId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order created successfully");
            response.put("data", order.toMap());
            response.put("razorpayKeyId", razorpayService.getPublicKeyId());
            response.put("key", razorpayService.getPublicKeyId());

            logger.info("[PAYMENT] create-order DONE razorpayOrderId={}", razorpayOrderId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("[PAYMENT] create-order FAILED: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create order");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestParam String orderId,
            @RequestParam String paymentId,
            @RequestParam String signature) {

        logger.info("[PAYMENT] verify START razorpayOrderId={} paymentId={}", orderId, paymentId);

        try {
            boolean success = razorpayService.verifyPayment(orderId, paymentId, signature);
            logger.info("[PAYMENT] verify razorpay signature result success={}", success);

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Payment successful" : "Payment failed");

                    if (success) {
                try {
                    logger.info("[PAYMENT] verify markOrderAsPaid START razorpayOrderId={}", orderId);
                    Order paidOrder = orderService.markOrderAsPaid(orderId, paymentId);
                    logger.info("[PAYMENT] verify markOrderAsPaid DONE orderNumber={}", paidOrder.getOrderNumber());

                    response.put("orderId", paidOrder.getId());
                    response.put("order_number", paidOrder.getOrderNumber());

                    try {
                        logger.info("[PAYMENT] verify Shiprocket createShipment START orderNumber={}", paidOrder.getOrderNumber());
                        ShiprocketShipmentResult sr = shiprocketService.createShipment(paidOrder);
                        logger.info("[PAYMENT] verify Shiprocket createShipment DONE shipmentId={} awb={}",
                                sr.getShipmentId(), sr.getAwbCode());

                        response.put("shipping_initiated", true);
                        response.put("shiprocket", sr.toMap());
                    } catch (Exception shippingError) {
                        logger.error("[PAYMENT] verify Shiprocket FAILED (payment still ok) orderNumber={}",
                                paidOrder.getOrderNumber(), shippingError);
                        response.put("shipping_initiated", false);
                        response.put("shipping_error", "Shiprocket order could not be created. Order is paid; retry shipment from admin.");
                        response.put("shipping_error_detail", shippingError.getMessage());
                    }
                } catch (Exception markPaidError) {
                    logger.error("[PAYMENT] verify markOrderAsPaid FAILED razorpayOrderId={}", orderId, markPaidError);
                    response.put("success", false);
                    response.put("message", "Payment verified but order update failed: " + markPaidError.getMessage());
                }
            }

            logger.info("[PAYMENT] verify END success={} keys={}", response.get("success"), response.keySet());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("[PAYMENT] verify ERROR: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error verifying payment");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
