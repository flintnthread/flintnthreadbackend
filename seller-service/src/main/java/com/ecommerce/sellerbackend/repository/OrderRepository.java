package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByIdIn(Collection<Long> ids);

    Optional<Order> findByOrderNumber(String orderNumber);
}
