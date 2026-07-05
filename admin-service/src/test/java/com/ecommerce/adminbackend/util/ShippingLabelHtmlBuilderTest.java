package com.ecommerce.adminbackend.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShippingLabelHtmlBuilderTest {

    @Test
    void buildIncludesShippingLabelSections() {
        Map<String, Object> company = new LinkedHashMap<>();
        company.put("name", "Flint & Thread");

        Map<String, Object> shipping = new LinkedHashMap<>();
        shipping.put("name", "Test Customer");
        shipping.put("line1", "12 Main Street");
        shipping.put("city", "Hyderabad");
        shipping.put("state", "Telangana");
        shipping.put("pincode", "500081");
        shipping.put("phone", "9999999999");

        Map<String, Object> product = new LinkedHashMap<>();
        product.put("name", "Cotton Shirt");
        product.put("hsnCode", "62052000");
        product.put("qty", 1);
        product.put("unitPrice", new BigDecimal("999.00"));
        product.put("taxPercent", new BigDecimal("5"));
        product.put("lineSubtotal", new BigDecimal("999.00"));
        product.put("taxAmount", new BigDecimal("49.95"));
        product.put("lineTotal", new BigDecimal("1048.95"));

        Map<String, Object> seller = new LinkedHashMap<>();
        seller.put("name", "Demo Seller");
        seller.put("address", Map.of(
                "line1", "Warehouse 1",
                "city", "Hyderabad",
                "state", "Telangana",
                "pincode", "500032"));

        Map<String, Object> sellerGroup = new LinkedHashMap<>();
        sellerGroup.put("seller", seller);
        sellerGroup.put("products", List.of(product));

        Map<String, Object> label = new LinkedHashMap<>();
        label.put("company", company);
        label.put("shipping", shipping);
        label.put("sellerGroups", List.of(sellerGroup));
        label.put("orderNumber", "ORD-95");
        label.put("invoiceNumber", "INV-95");
        label.put("orderDate", LocalDateTime.of(2026, 3, 1, 10, 0));
        label.put("courierName", "BlueDart");
        label.put("awbCode", "AWB123456");
        label.put("isIntraState", true);
        label.put("weightKg", new BigDecimal("0.50"));
        label.put("dimensionsCm", Map.of("l", 10, "w", 8, "h", 5));
        label.put("payment", Map.of("method", "cod"));
        label.put("totals", Map.of("grandTotal", new BigDecimal("1048.95")));
        label.put("gstBreakdown", Map.of("cgst", 25, "sgst", 25, "igst", 0));

        String html = ShippingLabelHtmlBuilder.build(label);

        assertTrue(html.contains("SHIPPING LABEL"));
        assertTrue(html.contains("Cotton Shirt"));
        assertTrue(html.contains("Test Customer"));
        assertTrue(html.contains("AWB123456"));
    }
}
