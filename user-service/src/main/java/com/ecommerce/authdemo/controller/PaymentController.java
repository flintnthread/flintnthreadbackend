package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.entity.Order;
import com.ecommerce.authdemo.service.OrderService;
import com.ecommerce.authdemo.service.RazorpayService;
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
            response.put("currency", razorpayService.getCurrency());
            response.put("companyName", razorpayService.getCompanyName());

            logger.info("[PAYMENT] create-order DONE razorpayOrderId={}", razorpayOrderId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("[PAYMENT] create-order FAILED: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            String detail = e.getMessage() != null ? e.getMessage() : "Unknown error";
            // Surface auth misconfig clearly (wrong key/secret causes opaque 500 on checkout).
            if (detail.toLowerCase().contains("auth")
                    || detail.toLowerCase().contains("authentication")
                    || detail.toLowerCase().contains("credentials")) {
                response.put("message", "Razorpay authentication failed. Check razorpay.key_id / razorpay.key_secret.");
            } else {
                response.put("message", "Failed to create payment order: " + detail);
            }
            response.put("error", detail);
            // HTTP 200 with success=false so checkout can show the message (not Axios 502).
            return ResponseEntity.ok(response);
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

                    // createShipment is scheduled async inside markOrderAsPaid (after commit).
                    boolean alreadyLinked = paidOrder.getShiprocketOrderId() != null
                            && !paidOrder.getShiprocketOrderId().isBlank();
                    response.put("shipping_initiated", alreadyLinked);
                    if (alreadyLinked) {
                        response.put("shiprocket_order_id", paidOrder.getShiprocketOrderId());
                        response.put("shiprocket_shipment_id", paidOrder.getShiprocketShipmentId());
                    } else {
                        // Payment success must not wait on Shiprocket (client timeout was 15s).
                        response.put("shipping_initiated", false);
                        response.put(
                                "shipping_note",
                                "Shipment is being created in the background. Refresh order shortly if tracking is empty."
                        );
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

    /**
     * Confirm payment when UPI/QR succeeded on phone but browser never received Razorpay handler callback.
     * Server checks Razorpay order status directly.
     */
    @PostMapping("/confirm-paid")
    public ResponseEntity<?> confirmPaid(@RequestParam String orderId) {
        logger.info("[PAYMENT] confirm-paid START razorpayOrderId={}", orderId);
        Map<String, Object> response = new HashMap<>();
        try {
            String paymentId = razorpayService.findCapturedPaymentId(orderId);
            if (paymentId == null || paymentId.isBlank()) {
                response.put("success", false);
                response.put("paid", false);
                response.put("message", "Payment not completed yet. Finish UPI/QR payment, then try again.");
                return ResponseEntity.ok(response);
            }

            Order paidOrder = orderService.markOrderAsPaid(orderId, paymentId);
            response.put("success", true);
            response.put("paid", true);
            response.put("message", "Payment successful");
            response.put("orderId", paidOrder.getId());
            response.put("order_number", paidOrder.getOrderNumber());
            boolean alreadyLinked = paidOrder.getShiprocketOrderId() != null
                    && !paidOrder.getShiprocketOrderId().isBlank();
            response.put("shipping_initiated", alreadyLinked);
            logger.info("[PAYMENT] confirm-paid DONE orderNumber={}", paidOrder.getOrderNumber());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[PAYMENT] confirm-paid FAILED razorpayOrderId={}", orderId, e);
            response.put("success", false);
            response.put("message", e.getMessage() != null ? e.getMessage() : "Could not confirm payment");
            return ResponseEntity.ok(response);
        }
    }
}
