package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.OrderItemCustomDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemCustomDetailRepository extends JpaRepository<OrderItemCustomDetail, Long> {

    List<OrderItemCustomDetail> findByOrderItemIdOrderByIdAsc(Long orderItemId);

    void deleteByOrderItemId(Long orderItemId);
}
