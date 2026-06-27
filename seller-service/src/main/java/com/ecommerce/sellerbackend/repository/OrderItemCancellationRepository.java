package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.OrderItemCancellation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrderItemCancellationRepository extends JpaRepository<OrderItemCancellation, Integer> {

    List<OrderItemCancellation> findByOrderItemIdInOrderByCreatedAtDesc(Collection<Integer> orderItemIds);
}
