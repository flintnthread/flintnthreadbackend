package com.ecommerce.authdemo.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShiprocketOrderPricingTest {

    @Test
    void shiprocketSubTotal_matchesOrderPlacingPrice() {
        var priced = ShiprocketOrderPricing.build(
                List.of(new ShiprocketOrderPricing.LineInput(
                        "Kurti", "K1", "6204", 1,
                        new BigDecimal("799.00"),
                        new BigDecimal("799.00")
                )),
                ShiprocketOrderPricing.toMoney(857.0),
                ShiprocketOrderPricing.toMoney(49.0),
                ShiprocketOrderPricing.toMoney(0.0),
                null
        );
        assertEquals(new BigDecimal("857.00"), priced.subTotal());
        assertEquals(new BigDecimal("0.00"), priced.shippingCharges());
        assertEquals(priced.orderTotal(), priced.subTotal());
    }
}
