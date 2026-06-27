package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.OrderGst;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderGstRepository extends JpaRepository<OrderGst, Integer> {

    List<OrderGst> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
