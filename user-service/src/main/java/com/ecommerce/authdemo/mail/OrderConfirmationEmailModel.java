package com.ecommerce.authdemo.mail;

import com.ecommerce.authdemo.dto.OrderItemDTO;

import java.util.List;

public record OrderConfirmationEmailModel(
        String customerName,
        String customerEmail,
        String orderNumber,
        String orderDate,
        String paymentMethodLabel,
        String paymentStatusLabel,
        double subtotal,
        double discount,
        double shipping,
        double walletUsed,
        double orderGrandTotal,
        double payable,
        String shippingAddress,
        String orderViewUrl,
        List<OrderItemDTO> items
) {
}
