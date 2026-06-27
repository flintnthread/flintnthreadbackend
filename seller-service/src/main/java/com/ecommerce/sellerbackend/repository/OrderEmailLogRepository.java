package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.OrderEmailLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderEmailLogRepository extends JpaRepository<OrderEmailLog, Integer> {

    List<OrderEmailLog> findByOrderIdOrderBySentAtDesc(Long orderId);
}
