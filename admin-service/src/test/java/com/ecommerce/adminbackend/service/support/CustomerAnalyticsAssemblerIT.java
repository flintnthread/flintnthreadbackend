package com.ecommerce.adminbackend.service.support;

import com.ecommerce.adminbackend.repository.CustomerQueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class CustomerAnalyticsAssemblerIT {

    @Autowired
    private CustomerAnalyticsAssembler assembler;

    @Autowired
    private CustomerQueryRepository customerQueryRepository;

    @Test
    void buildAnalyticsForCustomer95() {
        Optional<Object[]> row = customerQueryRepository.findCustomerById(95L);
        assertNotNull(row.orElseThrow());
        Map<String, Object> result = assembler.build(95L, row.get());
        assertNotNull(result.get("totalOrders"));
    }
}
