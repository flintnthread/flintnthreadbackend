package com.ecommerce.authdemo.repository;


import com.ecommerce.authdemo.entity.ReturnExchange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

    public interface ReturnExchangeRepository
            extends JpaRepository<ReturnExchange, Long> {

        List<ReturnExchange> findByUserId(Long userId);

        Optional<ReturnExchange> findByOrderItemId(Long orderItemId);
    }

