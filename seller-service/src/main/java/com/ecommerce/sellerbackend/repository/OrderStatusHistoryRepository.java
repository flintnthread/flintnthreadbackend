package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    Optional<OrderStatusHistory> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);
}
