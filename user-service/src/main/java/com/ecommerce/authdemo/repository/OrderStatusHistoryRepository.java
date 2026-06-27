package com.ecommerce.authdemo.repository;



import com.ecommerce.authdemo.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

    @Repository
    public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

        List<OrderStatusHistory> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    }

