package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import com.ecommerce.adminbackend.entity.DeliveryCharge;
import com.ecommerce.adminbackend.service.DeliverySlabAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/delivery-slabs")
@RequiredArgsConstructor
public class DeliverySlabAdminController {

    private static final Logger log = LogFactory.getLogger(DeliverySlabAdminController.class);

    private final DeliverySlabAdminService deliverySlabAdminService;

    @GetMapping
    public List<Map<String, Object>> list() {
        return deliverySlabAdminService.listSlabs();
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody DeliveryCharge request) {
        return deliverySlabAdminService.create(request);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Integer id, @RequestBody DeliveryCharge request) {
        return deliverySlabAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        deliverySlabAdminService.delete(id);
    }
}
