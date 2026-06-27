package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.RefundRequestDTO;
import com.ecommerce.authdemo.dto.RefundResponseDTO;

import com.ecommerce.authdemo.service.RefundService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

    @RestController
    @RequestMapping("/api/refunds")
    @RequiredArgsConstructor
    public class RefundController {

        private final RefundService refundService;


        @PostMapping
        public RefundResponseDTO processRefund(

                @RequestBody
                RefundRequestDTO dto
        ) {

            return refundService.processRefund(
                    dto
            );
        }


        @GetMapping
        public List<RefundResponseDTO>
        getUserRefunds() {

            return refundService.getUserRefunds();
        }

        @GetMapping("/order/{orderId}")
        public List<RefundResponseDTO>
        getOrderRefunds(

                @PathVariable Long orderId
        ) {

            return refundService.getOrderRefunds(
                    orderId
            );
        }
    }

