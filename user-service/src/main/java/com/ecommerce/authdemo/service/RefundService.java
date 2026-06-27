package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.RefundRequestDTO;
import com.ecommerce.authdemo.dto.RefundResponseDTO;

import java.util.List;

    public interface RefundService {

        RefundResponseDTO processRefund(
                RefundRequestDTO dto
        );

        List<RefundResponseDTO> getUserRefunds();

        List<RefundResponseDTO> getOrderRefunds(
                Long orderId
        );
    }

