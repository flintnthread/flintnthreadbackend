package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Query("""
        select case when count(oi.id) > 0 then true else false end
        from OrderItem oi
        join Order o on o.id = oi.orderId
        where o.userId = :userId
          and oi.productId = :productId
          and lower(coalesce(o.orderStatus, '')) <> 'cancelled'
        """)
    boolean existsPurchasedProductForUser(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.sellerId = :sellerId")
    long countBySellerId(@Param("sellerId") Long sellerId);

    @Query("""
            SELECT COUNT(oi) FROM OrderItem oi
            WHERE oi.sellerId = :sellerId
              AND LOWER(COALESCE(oi.status, '')) IN ('delivered', 'completed', 'shipped')
            """)
    long countFulfilledBySellerId(@Param("sellerId") Long sellerId);
}