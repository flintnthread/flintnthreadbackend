package com.ecommerce.authdemo.controller;


import com.ecommerce.authdemo.entity.ProductView;
import com.ecommerce.authdemo.repository.ProductViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
    @RequestMapping("/api/views")
    @RequiredArgsConstructor
    public class ProductViewController {

        private final ProductViewRepository viewRepository;

        @PostMapping
        public void track(@RequestBody ProductView view) {
            viewRepository.save(view);
        }
    }

