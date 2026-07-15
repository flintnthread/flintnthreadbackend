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
            SELECT LOWER(COALESCE(o.order_status, oi.status, 'pending')), COUNT(DISTINCT oi.order_id)
            FROM order_items oi
            LEFT JOIN orders o ON o.id = oi.order_id
            WHERE oi.seller_id = :sellerId
            GROUP BY LOWER(COALESCE(o.order_status, oi.status, 'pending'))
            """, nativeQuery = true)
    List<Object[]> countOrdersByStatusForSeller(@Param("sellerId") Long sellerId);

    @Query(value = """
            SELECT COUNT(DISTINCT oi.order_id)
            FROM order_items oi
            LEFT JOIN orders o ON o.id = oi.order_id
            WHERE (:sellerId IS NULL OR oi.seller_id = :sellerId)
              AND COALESCE(o.created_at, oi.created_at) >= :startDate
              AND COALESCE(o.created_at, oi.created_at) <= :endDate
            """, nativeQuery = true)
    long countOrdersPlacedInPeriod(@Param("sellerId") Long sellerId,
                                   @Param("startDate") java.time.LocalDateTime startDate,
                                   @Param("endDate") java.time.LocalDateTime endDate);

    @Query(value = """
            SELECT COUNT(DISTINCT oi.order_id)
            FROM order_items oi
            WHERE (:sellerId IS NULL OR oi.seller_id = :sellerId)
            """, nativeQuery = true)
    long countDistinctOrdersForSeller(@Param("sellerId") Long sellerId);
}
