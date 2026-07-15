package com.ecommerce.authdemo.mail;

import com.ecommerce.authdemo.dto.OrderItemDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderConfirmationEmailBuilderTest {

    @Test
    void buildHtml_forCustomerRecipient_usesCustomerCopy() {
        OrderConfirmationEmailModel model = OrderConfirmationEmailModel.forCustomer(
                "Jane Buyer",
                "buyer@example.com",
                "ORD-1000",
                "15-Jul-2026, 10:30 AM",
                "Razorpay",
                "Paid",
                1000.0,
                50.0,
                40.0,
                0.0,
                990.0,
                990.0,
                "123 Main Street",
                "https://flintnthread.in/order-view?orderId=1",
                List.of(OrderItemDTO.builder().productName("Shirt").quantity(1).total(1000.0).build())
        );

        String html = OrderConfirmationEmailBuilder.buildHtml(model);

        assertTrue(html.contains("Order Confirmed!"));
        assertTrue(html.contains("Thank you for your purchase"));
    }

    @Test
    void buildHtml_forSellerRecipient_usesSellerCopy() {
        OrderConfirmationEmailModel model = new OrderConfirmationEmailModel(
                "Jane Buyer",
                "seller@example.com",
                "ORD-1001",
                "15-Jul-2026, 10:30 AM",
                "Razorpay",
                "Paid",
                1000.0,
                50.0,
                40.0,
                0.0,
                990.0,
                990.0,
                "123 Main Street",
                "https://flintnthread.in/order-view?orderId=1",
                List.of(OrderItemDTO.builder().productName("Shirt").quantity(1).total(1000.0).build()),
                "seller",
                "Ravi Kumar"
        );

        String html = OrderConfirmationEmailBuilder.buildHtml(model);

        assertTrue(html.contains("New Order Received!"));
        assertTrue(html.contains("Your products from this order are now being processed"));
    }

    @Test
    void buildHtml_forAdminRecipient_usesAdminCopy() {
        OrderConfirmationEmailModel model = new OrderConfirmationEmailModel(
                "Jane Buyer",
                "admin@example.com",
                "ORD-1002",
                "15-Jul-2026, 10:30 AM",
                "Razorpay",
                "Paid",
                1000.0,
                0.0,
                40.0,
                0.0,
                1040.0,
                1040.0,
                "123 Main Street",
                "https://flintnthread.in/order-view?orderId=2",
                List.of(OrderItemDTO.builder().productName("Shirt").quantity(1).total(1000.0).build()),
                "admin",
                "Admin Team"
        );

        String html = OrderConfirmationEmailBuilder.buildHtml(model);

        assertTrue(html.contains("New Order Notification"));
        assertTrue(html.contains("A new customer order has been placed"));
    }
}
