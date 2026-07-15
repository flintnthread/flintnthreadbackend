package com.ecommerce.authdemo.mail;

import com.ecommerce.authdemo.dto.OrderItemDTO;

import java.util.List;
import java.util.Locale;

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
        List<OrderItemDTO> items,
        String recipientType,
        String recipientName
) {
    public static final String RECIPIENT_CUSTOMER = "customer";
    public static final String RECIPIENT_SELLER = "seller";
    public static final String RECIPIENT_ADMIN = "admin";

    public OrderConfirmationEmailModel {
        if (recipientType == null || recipientType.isBlank()) {
            recipientType = RECIPIENT_CUSTOMER;
        } else {
            recipientType = recipientType.trim().toLowerCase(Locale.ROOT);
        }
        if (recipientName == null || recipientName.isBlank()) {
            recipientName = customerName;
        }
    }

    /** Customer confirmation — backward-compatible factory. */
    public static OrderConfirmationEmailModel forCustomer(
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
        return new OrderConfirmationEmailModel(
                customerName,
                customerEmail,
                orderNumber,
                orderDate,
                paymentMethodLabel,
                paymentStatusLabel,
                subtotal,
                discount,
                shipping,
                walletUsed,
                orderGrandTotal,
                payable,
                shippingAddress,
                orderViewUrl,
                items,
                RECIPIENT_CUSTOMER,
                customerName
        );
    }
}
