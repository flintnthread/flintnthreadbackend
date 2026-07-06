package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.*;
import com.ecommerce.authdemo.service.OrderService;
import com.ecommerce.authdemo.service.OrderItemCustomDetailService;
import com.ecommerce.authdemo.exception.OrderException;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.OrderItemRepository;
import com.ecommerce.authdemo.service.ShiprocketService;
import com.ecommerce.authdemo.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin("*")
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final ShiprocketService
            shiprocketService;
    private final OrderItemCustomDetailService orderItemCustomDetailService;
    private final OrderItemRepository orderItemRepository;
    private final SecurityUtil securityUtil;

    @PostMapping("/place")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> placeOrder(
            @Valid @RequestBody PlaceOrderRequestDTO dto) {
        
        log.info("Place order request: addressId={}, paymentMethod={}", 
                dto.getAddressId(), dto.getPaymentMethod());
        
        try {
            OrderResponseDTO response = orderService.placeOrder(dto);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Order placed successfully", response)
            );
        } catch (OrderException e) {
            log.error("Order placement failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (ResourceNotFoundException e) {
            log.error("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error placing order: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, e.getMessage() != null ? e.getMessage() : "Failed to place order", null));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getOrders(
            @RequestParam(required = false) String status) {
        
        log.info("[ORDER:API] GET /api/orders status={}", status);
        
        try {
            List<OrderResponseDTO> orders = orderService.getUserOrders(status);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Orders fetched successfully", orders)
            );
        } catch (RuntimeException e) {
            String message = e.getMessage() != null ? e.getMessage() : "Failed to fetch orders";
            String lowered = message.toLowerCase();
            if (lowered.contains("not authenticated")
                    || lowered.contains("user not found")
                    || lowered.contains("access denied")) {
                log.warn("Unauthorized orders request: {}", message);
                return ResponseEntity.status(401)
                        .body(new ApiResponse<>(false, message, List.of()));
            }
            log.error("Error fetching orders: {}", message, e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to fetch orders", List.of()));
        } catch (Exception e) {
            log.error("Error fetching orders: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to fetch orders", List.of()));
        }
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getPublicOrderDetails(
            @PathVariable Long id,
            @RequestParam String orderNumber,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer lineIndex,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) String productIds) {

        log.info("Public order view request: orderId={}", id);

        if (id == null || id <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Valid order ID is required", null));
        }
        if (orderNumber == null || orderNumber.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Order number is required", null));
        }

        try {
            OrderResponseDTO order = orderService.getPublicOrderDetails(
                    id, orderNumber, productId, lineIndex, sellerId, productIds
            );
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Order details fetched successfully", order)
            );
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (OrderException e) {
            return ResponseEntity.status(403)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error fetching public order details: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to fetch order details", null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderDetails(
            @PathVariable Long id) {
        
        log.info("Fetch order details request: orderId={}", id);
        
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Valid order ID is required", null));
        }
        
        try {
            OrderResponseDTO order = orderService.getOrderDetails(id);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Order details fetched successfully", order)
            );
        } catch (ResourceNotFoundException e) {
            log.error("Order not found: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (OrderException e) {
            log.error("Order access error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error fetching order details: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to fetch order details", null));
        }
    }


    @GetMapping("/{orderId}/custom-details")
    public ResponseEntity<ApiResponse<OrderCustomDetailsResponseDTO>> getOrderCustomDetails(
            @PathVariable Long orderId) {
        if (orderId == null || orderId <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Valid order ID is required", null));
        }
        try {
            Long userId = securityUtil.getCurrentUserId();
            OrderCustomDetailsResponseDTO data =
                    orderItemCustomDetailService.getOrderCustomDetails(orderId, userId);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Customization details fetched", data)
            );
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (OrderException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error fetching order custom details: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to fetch customization details", null));
        }
    }

    @PostMapping("/items/{orderItemId}/custom-details")
    public ResponseEntity<ApiResponse<OrderCustomDetailsResponseDTO>> saveOrderItemCustomDetails(
            @PathVariable Long orderItemId,
            @Valid @RequestBody SaveOrderItemCustomDetailsRequestDTO dto) {
        if (orderItemId == null || orderItemId <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Valid order item ID is required", null));
        }
        try {
            Long userId = securityUtil.getCurrentUserId();
            orderItemCustomDetailService.saveOrderItemCustomDetails(
                    orderItemId,
                    userId,
                    dto != null ? dto.getFields() : null
            );
            Long orderId = orderItemRepository.findById(orderItemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order item not found"))
                    .getOrderId();
            OrderCustomDetailsResponseDTO refreshed =
                    orderItemCustomDetailService.getOrderCustomDetails(orderId, userId);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Customization details saved", refreshed)
            );
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (OrderException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error saving order custom details: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to save customization details", null));
        }
    }


    @GetMapping("/tracking/{awb}")
    public OrderTrackingResponseDTO
    trackShipment(

            @PathVariable String awb
    ) {

        return shiprocketService
                .getTrackingDetails(awb);
    }

    @PostMapping(
            "/retry-payment"
    )
    public ResponseEntity<?> retryPayment(
            @RequestBody
            RetryPaymentRequestDTO dto
    ) {

        return ResponseEntity.ok(
                orderService.retryPayment(
                        dto.getOrderId()
                )
        );
    }

    @PostMapping(
            "/verify-retry-payment"
    )
    public ResponseEntity<?> verifyRetryPayment(
            @RequestBody
            VerifyPaymentRequestDTO dto
    ) {

        return ResponseEntity.ok(
                orderService.verifyRetryPayment(dto)
        );
    }




    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> body
    ) {

        String cancelReason = body.get("reason");
        boolean refundToWallet = parseRefundToWallet(body.get("refundToWallet"));

        CancelOrderResponseDTO result =
                orderService.cancelOrder(orderId, cancelReason, refundToWallet);

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", result.getMessage(),
                        "walletCredited", result.isWalletCredited(),
                        "walletCreditAmount", result.getWalletCreditAmount()
                )
        );
    }

    private static boolean parseRefundToWallet(Object raw) {
        if (raw == null) {
            return true;
        }
        if (raw instanceof Boolean b) {
            return b;
        }
        String text = String.valueOf(raw).trim().toLowerCase();
        if (text.isEmpty()) {
            return true;
        }
        return "true".equals(text)
                || "1".equals(text)
                || "yes".equals(text);
    }

}