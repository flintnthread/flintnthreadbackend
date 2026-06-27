package com.ecommerce.authdemo.repository;


import com.ecommerce.authdemo.entity.RefundTransaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

    public interface RefundTransactionRepository
            extends JpaRepository<RefundTransaction, Long> {

        List<RefundTransaction>
        findByUserId(Long userId);

        List<RefundTransaction>
        findByOrderId(Long orderId);
    }

