package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.OrderExchange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrderExchangeRepository extends JpaRepository<OrderExchange, Integer> {

    List<OrderExchange> findByOrderItemIdInOrderByCreatedAtDesc(Collection<Integer> orderItemIds);
}
