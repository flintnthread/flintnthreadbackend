package com.ecommerce.adminbackend.service.shiprocket;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShiprocketOrderPricingTest {

    @Test
    void subTotal_equalsExactOrderPlacingTotal() {
        // Checkout: products 998 + shipping 49 + fee 9 = 1056
        var priced = ShiprocketOrderPricing.build(
                List.of(new ShiprocketOrderPricing.LineInput(
                        "Shirt", "SKU1", "6109", 2,
                        new BigDecimal("499.00"),
                        new BigDecimal("998.00")
                )),
                new BigDecimal("1056.00"),
                new BigDecimal("49.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        assertEquals(new BigDecimal("1056.00"), priced.subTotal());
        assertEquals(new BigDecimal("1056.00"), priced.orderTotal());
        assertEquals(new BigDecimal("0.00"), priced.shippingCharges());
        assertEquals(priced.orderTotal(), priced.subTotal());
    }

    @Test
    void discount_keepsSubTotalEqualToOrderTotal() {
        var priced = ShiprocketOrderPricing.build(
                List.of(new ShiprocketOrderPricing.LineInput(
                        "Shoes", "SKU2", "6403", 1,
                        new BigDecimal("2000.00"),
                        new BigDecimal("2000.00")
                )),
                new BigDecimal("1850.00"),
                new BigDecimal("50.00"),
                new BigDecimal("200.00"),
                BigDecimal.ZERO
        );

        assertEquals(new BigDecimal("1850.00"), priced.subTotal());
        assertEquals(new BigDecimal("0.00"), priced.shippingCharges());
        assertEquals(new BigDecimal("150.00"), priced.totalDiscount());
        assertEquals(priced.orderTotal(), priced.subTotal());
    }

    @Test
    void usesItemTotalsWhenOrderTotalMissing() {
        var priced = ShiprocketOrderPricing.build(
                List.of(new ShiprocketOrderPricing.LineInput(
                        "Dress", "SKU3", "6204", 1,
                        BigDecimal.ZERO,
                        new BigDecimal("799.00")
                )),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        assertEquals(new BigDecimal("799.00"), priced.subTotal());
        assertEquals(new BigDecimal("799.00"), ShiprocketOrderPricing.computeGrandTotal(priced));
    }
}
