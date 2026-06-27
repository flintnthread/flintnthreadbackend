package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.OrderReplacement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrderReplacementRepository extends JpaRepository<OrderReplacement, Integer> {

    List<OrderReplacement> findByOrderItemIdInOrderByCreatedAtDesc(Collection<Integer> orderItemIds);
}
