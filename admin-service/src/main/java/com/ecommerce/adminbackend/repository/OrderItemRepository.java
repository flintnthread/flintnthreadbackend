package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByOrderIdIn(Collection<Long> orderIds);

    @Query(value = """
            SELECT oi.product_id,
                   p.name as product_name,
                   COALESCE(SUM(oi.quantity), 0),
                   COALESCE(SUM(oi.total), 0)
            FROM order_items oi
            INNER JOIN products p ON p.id = oi.product_id
            WHERE oi.product_id IS NOT NULL
            GROUP BY oi.product_id, p.name
            ORDER BY SUM(oi.quantity) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopSellingProducts(@Param("limit") int limit);

    @Query(value = """
            SELECT oi.seller_id,
                   MAX(oi.seller_name),
                   COUNT(DISTINCT oi.order_id),
                   COALESCE(SUM(oi.total), 0)
            FROM order_items oi
            WHERE oi.seller_id IS NOT NULL
            GROUP BY oi.seller_id
            ORDER BY SUM(oi.total) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopSellers(@Param("limit") int limit);

    @Query(value = """
            SELECT oi.seller_id, COALESCE(SUM(oi.total), 0)
            FROM order_items oi
            WHERE oi.seller_id IS NOT NULL
            GROUP BY oi.seller_id
            """, nativeQuery = true)
    List<Object[]> sumRevenueBySeller();

    @Query(value = """
            SELECT oi.seller_id, COUNT(DISTINCT oi.order_id)
            FROM order_items oi
            WHERE oi.seller_id IS NOT NULL
            GROUP BY oi.seller_id
            """, nativeQuery = true)
    List<Object[]> countOrdersBySeller();

    @Query(value = """
            SELECT LOWER(o.order_status), COUNT(DISTINCT o.id)
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE oi.seller_id = :sellerId
            GROUP BY LOWER(o.order_status)
            """, nativeQuery = true)
    List<Object[]> countOrdersByStatusForSeller(@Param("sellerId") Long sellerId);
}
