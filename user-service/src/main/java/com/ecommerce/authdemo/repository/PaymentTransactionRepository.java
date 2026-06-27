package com.ecommerce.authdemo.repository;


import com.ecommerce.authdemo.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Integer> {

        List<PaymentTransaction> findByOrderId(Integer orderId);

        PaymentTransaction findByTransactionId(String transactionId);

        Optional<PaymentTransaction>
        findTopByOrderIdOrderByIdDesc(
                Long orderId
        );
    }

