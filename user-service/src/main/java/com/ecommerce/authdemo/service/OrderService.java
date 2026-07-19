package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.CancelOrderResponseDTO;
import com.ecommerce.authdemo.dto.OrderResponseDTO;
import com.ecommerce.authdemo.dto.PlaceOrderRequestDTO;
import com.ecommerce.authdemo.dto.RetryPaymentResponseDTO;
import com.ecommerce.authdemo.dto.ShiprocketShipmentResult;
import com.ecommerce.authdemo.dto.UpdateOrderAddressRequestDTO;
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

    OrderResponseDTO updateOrderAddress(Long orderId, UpdateOrderAddressRequestDTO dto);

    Order markOrderAsPaid(String razorpayOrderId, String paymentId);

    /** Mark unpaid online order as payment_failed after Razorpay cancel/fail. */
    Order markOrderPaymentFailed(String razorpayOrderId);

    /**
     * System-initiated cancel for an unpaid online order whose payment window
     * expired. Restores stock, refunds any wallet actually debited, and notifies
     * the shopper. Runs without a security context (used by the cleanup job).
     */
    Order systemCancelUnpaidOrder(Long orderId, String reason);

    /** Retry Shiprocket create for a paid/COD order that was not linked yet. */
    ShiprocketShipmentResult pushOrderToShiprocket(Long orderId);

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