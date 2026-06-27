package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ReturnOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

    public interface ReturnOrderRepository
            extends JpaRepository<ReturnOrder, Long> {

        List<ReturnOrder> findByUserId(Long userId);

        Optional<ReturnOrder> findByOrderItemId(Long orderItemId);
    }

