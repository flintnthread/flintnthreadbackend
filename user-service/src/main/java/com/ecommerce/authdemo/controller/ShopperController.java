package com.ecommerce.authdemo.controller;


import com.ecommerce.authdemo.dto.CreateShopperDTO;
import com.ecommerce.authdemo.service.ShopperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
    @RequestMapping("/api/shoppers")
    @RequiredArgsConstructor
    public class ShopperController {

        private final ShopperService shopperService;

        @GetMapping
        public ResponseEntity<?> getAll() {
            return ResponseEntity.ok(shopperService.getAll());
        }

        @PostMapping
        public ResponseEntity<?> create(@Valid @RequestBody CreateShopperDTO dto) {
            return ResponseEntity.ok(shopperService.create(dto));
        }

        @PutMapping("/{id}/activate")
        public ResponseEntity<?> activate(@PathVariable Integer id) {
            shopperService.activate(id);
            return ResponseEntity.ok("Shopper switched successfully");
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<?> delete(@PathVariable Integer id) {
            shopperService.delete(id);
            return ResponseEntity.ok("Shopper deleted");
        }
    }

