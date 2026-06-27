package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import com.ecommerce.adminbackend.common.PageResponse;
import com.ecommerce.adminbackend.service.CustomerAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
public class CustomerAdminController {

    private static final Logger log = LogFactory.getLogger(CustomerAdminController.class);

    private final CustomerAdminService customerAdminService;

    @GetMapping
    public PageResponse<Map<String, Object>> listCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return customerAdminService.listCustomers(search, page, size);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return customerAdminService.stats();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getCustomer(@PathVariable Long id) {
        return customerAdminService.getCustomer(id);
    }

    @GetMapping("/{id}/analytics")
    public Map<String, Object> getCustomerAnalytics(@PathVariable Long id) {
        return customerAdminService.getCustomerAnalytics(id);
    }
}
