package com.ecommerce.authdemo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
@CrossOrigin("*")
public class IndexController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> index() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "FlintnThread E-commerce API Server");
        response.put("version", "1.0.0");
        response.put("status", "running");
        response.put("endpoints", Map.of(
            "orders", "/api/orders",
            "products", "/api/products",
            "auth", "/api/auth",
            "cart", "/api/cart",
            "payment", "/api/payment",
            "reviews", "/api/reviews",
            "returns", "/api/returns",
            "exchanges", "/api/exchanges",
            "shiprocket", "/api/shiprocket"
        ));
        
        return ResponseEntity.ok(response);
    }
}
