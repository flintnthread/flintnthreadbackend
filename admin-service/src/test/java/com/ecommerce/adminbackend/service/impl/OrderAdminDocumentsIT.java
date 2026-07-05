package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.service.OrderAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class OrderAdminDocumentsIT {

    @Autowired
    private OrderAdminService orderAdminService;

    @Test
    void generateInvoiceForOrder95() {
        Map<String, Object> invoice = orderAdminService.generateInvoice(95L);
        assertNotNull(invoice.get("invoiceNumber"));
        assertNotNull(invoice.get("sellerGroups"));
        assertFalse(String.valueOf(invoice.get("orderNumber")).isBlank());
    }

    @Test
    void generateInvoicePdfForOrder95() {
        byte[] pdf = orderAdminService.generateInvoicePdf(95L);
        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
        assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F');
    }

    @Test
    void generateShippingLabelForOrder95() {
        Map<String, Object> label = orderAdminService.generateShippingLabel(95L);
        assertNotNull(label.get("invoiceNumber"));
        assertNotNull(label.get("sellerGroups"));
        assertNotNull(label.get("shipping"));
        assertFalse(String.valueOf(label.get("courierName")).isBlank());
    }

    @Test
    void generateShippingLabelPdfForOrder95() {
        byte[] pdf = orderAdminService.generateShippingLabelPdf(95L);
        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
        assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F');
    }

    @Test
    void exportOrdersCsvContainsHeaders() {
        String csv = orderAdminService.exportOrdersCsv(null, null, null, null, null);
        assertNotNull(csv);
        assertTrue(csv.contains("Order ID"));
        assertTrue(csv.contains("Order Number"));
    }

    @Test
    void updateOrderStatusWithNotifyCustomerDoesNotFail() {
        Map<String, Object> updated = orderAdminService.updateOrderStatus(
                95L,
                "Processing",
                "Integration test status update",
                1L,
                true);
        assertNotNull(updated.get("id"));
        assertNotNull(updated.get("statusHistory"));
    }
}
