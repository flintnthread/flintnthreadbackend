package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.OrderReturn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrderReturnRepository extends JpaRepository<OrderReturn, Integer> {

    List<OrderReturn> findByOrderItemIdInOrderByCreatedAtDesc(Collection<Integer> orderItemIds);
}
