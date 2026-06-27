package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.entity.Order;
import com.ecommerce.authdemo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists order cancellation in its own transaction so wallet refund failures
 * cannot roll back the cancelled order status.
 */
@Service
@RequiredArgsConstructor
public class OrderCancelPersistenceService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order markOrderCancelled(Order order) {
        order.setOrderStatus("cancelled");
        return orderRepository.saveAndFlush(order);
    }
}
