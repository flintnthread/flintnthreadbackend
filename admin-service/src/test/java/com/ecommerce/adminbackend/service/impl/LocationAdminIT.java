package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.service.LocationAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class LocationAdminIT {

    @Autowired
    private LocationAdminService locationAdminService;

    @Test
    void createCountryWithCode() {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String name = "Testland " + suffix;
        String code = suffix.substring(0, 2).toUpperCase();

        Map<String, Object> created = locationAdminService.createCountry(name, code, true);
        assertNotNull(created.get("id"));
        assertEquals(name, created.get("name"));
        assertEquals(code, created.get("code"));

        Integer id = ((Number) created.get("id")).intValue();
        locationAdminService.deleteCountry(id);
    }
}
