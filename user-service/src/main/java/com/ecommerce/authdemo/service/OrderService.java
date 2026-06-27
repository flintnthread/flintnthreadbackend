package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.CancelOrderResponseDTO;
import com.ecommerce.authdemo.dto.OrderResponseDTO;
import com.ecommerce.authdemo.dto.PlaceOrderRequestDTO;
import com.ecommerce.authdemo.dto.RetryPaymentResponseDTO;
import com.ecommerce.authdemo.dto.VerifyPaymentRequestDTO;
import com.ecommerce.authdemo.entity.Order;

import java.util.List;

public interface OrderService {

    OrderResponseDTO placeOrder(PlaceOrderRequestDTO dto);

    List<OrderResponseDTO> getUserOrders(String status);

    OrderResponseDTO getOrderDetails(Long orderId);

    /** Invoice QR / shared link — no login; requires matching order number. */
    OrderResponseDTO getPublicOrderDetails(
            Long orderId,
            String orderNumber,
            Long productId,
            Integer lineIndex,
            Long sellerId,
            String productIds
    );

    CancelOrderResponseDTO cancelOrder(Long orderId, String cancelReason, boolean refundToWallet);

    Order markOrderAsPaid(String razorpayOrderId, String paymentId);

    void updateShipment(String orderNumber, String awb, String courier, String trackingUrl, String shiprocketStatus);

    void markShiprocketCreateFailed(String orderNumber, String reason);

    void updateOrderStatusFromWebhook(String awb, String status);

    void linkRazorpayOrder(String razorpayOrderId);

    void save(Order order);

    RetryPaymentResponseDTO retryPayment(
            Long orderId
    );

    boolean verifyRetryPayment(
            VerifyPaymentRequestDTO dto
    );
}