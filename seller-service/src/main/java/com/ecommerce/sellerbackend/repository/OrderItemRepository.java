package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    List<OrderItem> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    List<OrderItem> findBySellerIdAndOrderId(Long sellerId, Long orderId);

    @Query("""
            SELECT oi.productId, SUM(COALESCE(oi.quantity, 0))
            FROM OrderItem oi
            WHERE oi.sellerId = :sellerId AND oi.productId IS NOT NULL
            GROUP BY oi.productId
            ORDER BY SUM(COALESCE(oi.quantity, 0)) DESC
            """)
    List<Object[]> topProductIdsByQuantity(@Param("sellerId") Long sellerId);

    @Query("""
            SELECT COALESCE(SUM(oi.total), 0)
            FROM OrderItem oi
            WHERE oi.sellerId = :sellerId
            AND oi.createdAt >= :from AND oi.createdAt < :to
            """)
    BigDecimal sumSalesBetween(
            @Param("sellerId") Long sellerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT COUNT(DISTINCT oi.orderId)
            FROM OrderItem oi
            WHERE oi.sellerId = :sellerId
            AND oi.createdAt >= :from AND oi.createdAt < :to
            """)
    long countDistinctOrdersBetween(
            @Param("sellerId") Long sellerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(
            value = """
                    SELECT DATE(created_at) AS d, COALESCE(SUM(total), 0) AS amt
                    FROM order_items
                    WHERE seller_id = :sellerId
                      AND created_at >= :from
                      AND created_at < :to
                    GROUP BY DATE(created_at)
                    ORDER BY DATE(created_at)
                    """,
            nativeQuery = true)
    List<Object[]> sumSalesGroupedByDay(
            @Param("sellerId") Long sellerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(
            value = """
                    SELECT DATE(created_at) AS d, COUNT(DISTINCT order_id) AS cnt
                    FROM order_items
                    WHERE seller_id = :sellerId
                      AND created_at >= :from
                      AND created_at < :to
                    GROUP BY DATE(created_at)
                    ORDER BY DATE(created_at)
                    """,
            nativeQuery = true)
    List<Object[]> countOrdersGroupedByDay(
            @Param("sellerId") Long sellerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(
            value = """
                    SELECT DATE(created_at) AS d, COALESCE(SUM(quantity), 0) AS units
                    FROM order_items
                    WHERE seller_id = :sellerId
                      AND created_at >= :from
                      AND created_at < :to
                    GROUP BY DATE(created_at)
                    ORDER BY DATE(created_at)
                    """,
            nativeQuery = true)
    List<Object[]> sumUnitsGroupedByDay(
            @Param("sellerId") Long sellerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(
            value = """
                    SELECT COALESCE(NULLIF(TRIM(o.payment_method), ''), 'Other') AS pm,
                           COALESCE(SUM(oi.total), 0) AS amt,
                           COUNT(DISTINCT oi.order_id) AS ord_cnt
                    FROM order_items oi
                    INNER JOIN orders o ON o.id = oi.order_id
                    WHERE oi.seller_id = :sellerId
                      AND oi.created_at >= :from
                      AND oi.created_at < :to
                    GROUP BY pm
                    ORDER BY amt DESC
                    """,
            nativeQuery = true)
    List<Object[]> sumSalesByPaymentMethod(
            @Param("sellerId") Long sellerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT COALESCE(SUM(oi.total), 0)
            FROM OrderItem oi
            WHERE oi.sellerId = :sellerId AND oi.orderId = :orderId
            """)
    java.math.BigDecimal sumTotalForSellerOrder(
            @Param("sellerId") Long sellerId,
            @Param("orderId") Long orderId);

    @Query(
            value = """
                    SELECT COALESCE(NULLIF(TRIM(status), ''), 'Other') AS st,
                           COALESCE(SUM(total), 0) AS amt,
                           COUNT(DISTINCT order_id) AS ord_cnt
                    FROM order_items
                    WHERE seller_id = :sellerId
                      AND created_at >= :from
                      AND created_at < :to
                    GROUP BY st
                    ORDER BY amt DESC
                    """,
            nativeQuery = true)
    List<Object[]> sumSalesByStatus(
            @Param("sellerId") Long sellerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(
            value = """
                    SELECT oi.id,
                           CASE
                             WHEN oi.product_name IS NOT NULL AND TRIM(oi.product_name) <> ''
                                  AND LOWER(TRIM(oi.product_name)) <> 'product' THEN TRIM(oi.product_name)
                             WHEN p.name IS NOT NULL AND TRIM(p.name) <> ''
                                  AND LOWER(TRIM(p.name)) <> 'product' THEN TRIM(p.name)
                             WHEN oi.sku IS NOT NULL AND TRIM(oi.sku) <> '' THEN TRIM(oi.sku)
                             ELSE 'Product'
                           END
                    FROM order_items oi
                    LEFT JOIN products p ON p.id = oi.product_id
                    WHERE oi.id IN (:itemIds)
                    """,
            nativeQuery = true)
    List<Object[]> findResolvedProductNamesByItemIds(@Param("itemIds") Collection<Integer> itemIds);
}
