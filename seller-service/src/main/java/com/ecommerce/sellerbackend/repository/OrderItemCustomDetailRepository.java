package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.OrderItemCustomDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrderItemCustomDetailRepository extends JpaRepository<OrderItemCustomDetail, Integer> {

    List<OrderItemCustomDetail> findByOrderItemIdInOrderByCreatedAtAsc(Collection<Integer> orderItemIds);
}
