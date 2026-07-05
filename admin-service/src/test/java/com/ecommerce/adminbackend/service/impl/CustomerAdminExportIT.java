package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.repository.CustomerQueryRepository;
import com.ecommerce.adminbackend.service.CustomerAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CustomerAdminExportIT {

    @Autowired
    private CustomerAdminService customerAdminService;

    @Autowired
    private CustomerQueryRepository customerQueryRepository;

    @Test
    void exportOrderHistoryCsvForCustomer95() {
        Optional<Object[]> row = customerQueryRepository.findCustomerById(95L);
        assertTrue(row.isPresent(), "Customer 95 must exist in test database");

        String csv = customerAdminService.exportOrderHistoryCsv(95L);
        assertNotNull(csv);
        assertTrue(csv.contains("Order ID"));
        assertTrue(csv.contains("Order #"));
    }

    @Test
    void exportOrderHistoryPdfForCustomer95() {
        Optional<Object[]> row = customerQueryRepository.findCustomerById(95L);
        assertTrue(row.isPresent(), "Customer 95 must exist in test database");

        byte[] pdf = customerAdminService.exportOrderHistoryPdf(95L);
        assertNotNull(pdf);
        assertTrue(pdf.length > 100, "PDF should not be empty");
        assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F', "PDF magic bytes");
    }
}
