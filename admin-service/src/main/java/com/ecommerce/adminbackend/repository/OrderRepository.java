package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            SELECT o FROM Order o
            WHERE (
                   :status IS NULL OR :status = ''
                   OR (
                        LOWER(:status) IN ('delivered', 'completed')
                        AND LOWER(COALESCE(o.orderStatus, '')) IN ('delivered', 'completed')
                   )
                   OR (
                        LOWER(:status) IN ('cancelled', 'canceled')
                        AND LOWER(COALESCE(o.orderStatus, '')) IN ('cancelled', 'canceled')
                   )
                   OR (
                        LOWER(:status) = 'returned'
                        AND LOWER(COALESCE(o.orderStatus, '')) IN ('returned', 'refunded', 'rto_delivered', 'rto_initiated', 'replacement')
                   )
                   OR (
                        LOWER(:status) = 'shipped'
                        AND LOWER(COALESCE(o.orderStatus, '')) IN ('shipped', 'in_transit', 'out_for_delivery', 'picked_up', 'ready_to_ship')
                   )
                   OR (
                        LOWER(:status) = 'processing'
                        AND LOWER(COALESCE(o.orderStatus, '')) IN ('processing', 'confirmed', 'packed', 'awb_assigned', 'pickup_scheduled', 'accepted')
                   )
                   OR (
                        LOWER(:status) = 'pending'
                        AND LOWER(COALESCE(o.orderStatus, '')) IN ('pending', 'awaiting_payment', 'awaiting_processing', 'sent_to_seller', 'new', 'placed')
                   )
                   OR LOWER(COALESCE(o.orderStatus, '')) = LOWER(:status)
              )
              AND (:paymentStatus IS NULL OR :paymentStatus = '' OR LOWER(o.paymentStatus) = LOWER(:paymentStatus))
              AND (:paymentMethod IS NULL OR :paymentMethod = '' OR
                   LOWER(COALESCE(o.paymentMethod, '')) LIKE LOWER(CONCAT('%', :paymentMethod, '%')))
              AND (:search IS NULL OR :search = '' OR
                   LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(o.shippingName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(o.shippingEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   o.shippingPhone LIKE CONCAT('%', :search, '%'))
              AND (:sellerId IS NULL OR EXISTS (
                   SELECT 1 FROM OrderItem oi WHERE oi.orderId = o.id AND oi.sellerId = :sellerId
              ))
            """)
    Page<Order> searchOrders(@Param("status") String status,
                             @Param("paymentStatus") String paymentStatus,
                             @Param("paymentMethod") String paymentMethod,
                             @Param("search") String search,
                             @Param("sellerId") Long sellerId,
                             Pageable pageable);

    @Query("""
            SELECT COUNT(o) FROM Order o
            WHERE LOWER(COALESCE(o.paymentStatus, '')) IN ('pending', 'unpaid', 'created', 'authorized')
            """)
    long countPendingPayments();

    long countByOrderStatusIgnoreCase(String orderStatus);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    BigDecimal sumTotalAmount();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt >= :since")
    BigDecimal sumTotalAmountSince(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt >= :start AND o.createdAt < :end")
    BigDecimal sumTotalAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :start AND o.createdAt < :end")
    long countBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o
            WHERE YEAR(o.createdAt) = :year AND MONTH(o.createdAt) = :month
            """)
    BigDecimal sumForMonth(@Param("year") int year, @Param("month") int month);

    @Query("""
            SELECT COUNT(o) FROM Order o
            WHERE YEAR(o.createdAt) = :year AND MONTH(o.createdAt) = :month
            """)
    long countForMonth(@Param("year") int year, @Param("month") int month);

    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o
            WHERE YEAR(o.createdAt) = :year
            """)
    BigDecimal sumForYear(@Param("year") int year);

    @Query("SELECT COUNT(o) FROM Order o WHERE YEAR(o.createdAt) = :year")
    long countForYear(@Param("year") int year);

    List<Order> findTop10ByOrderByCreatedAtDesc();

    /**
     * Seller Payments: delivered/completed orders only (null seller_payment_status = pending).
     * Reminder day counts use delivery reference = COALESCE(shiprocket_synced_at, updated_at).
     */
    @Query(value = """
            SELECT * FROM orders o
            WHERE (
                LOWER(CAST(o.order_status AS CHAR)) IN ('delivered', 'completed')
                OR LOWER(COALESCE(o.shiprocket_status, '')) IN ('7', '23', '26', 'delivered', 'completed', 'fulfilled')
              )
              AND LOWER(CAST(COALESCE(o.order_status, 'pending') AS CHAR)) NOT IN (
                   'cancelled', 'canceled', 'returned', 'refunded',
                   'rto_delivered', 'rto_initiated', 'replacement'
              )
              AND (
                :status IS NULL OR :status = ''
                OR (
                  LOWER(:status) = 'pending'
                  AND (o.seller_payment_status IS NULL OR LOWER(o.seller_payment_status) = 'pending')
                )
                OR (
                  LOWER(:status) = 'paid-cancelled'
                  AND LOWER(COALESCE(o.seller_payment_status, '')) IN ('paid', 'cancelled')
                )
                OR (
                  LOWER(:status) NOT IN ('pending', 'paid-cancelled')
                  AND LOWER(COALESCE(o.seller_payment_status, '')) = LOWER(:status)
                )
              )
            ORDER BY
              CASE
                WHEN o.seller_payment_status IS NULL OR LOWER(o.seller_payment_status) = 'pending' THEN 0
                WHEN LOWER(o.seller_payment_status) = 'paid' THEN 1
                ELSE 2
              END,
              COALESCE(o.shiprocket_synced_at, o.updated_at) ASC,
              o.id DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM orders o
            WHERE (
                LOWER(CAST(o.order_status AS CHAR)) IN ('delivered', 'completed')
                OR LOWER(COALESCE(o.shiprocket_status, '')) IN ('7', '23', '26', 'delivered', 'completed', 'fulfilled')
              )
              AND LOWER(CAST(COALESCE(o.order_status, 'pending') AS CHAR)) NOT IN (
                   'cancelled', 'canceled', 'returned', 'refunded',
                   'rto_delivered', 'rto_initiated', 'replacement'
              )
              AND (
                :status IS NULL OR :status = ''
                OR (
                  LOWER(:status) = 'pending'
                  AND (o.seller_payment_status IS NULL OR LOWER(o.seller_payment_status) = 'pending')
                )
                OR (
                  LOWER(:status) = 'paid-cancelled'
                  AND LOWER(COALESCE(o.seller_payment_status, '')) IN ('paid', 'cancelled')
                )
                OR (
                  LOWER(:status) NOT IN ('pending', 'paid-cancelled')
                  AND LOWER(COALESCE(o.seller_payment_status, '')) = LOWER(:status)
                )
              )
            """,
            nativeQuery = true)
    Page<Order> findSellerPayments(@Param("status") String status, Pageable pageable);

    @Query(value = """
            SELECT COUNT(*) FROM orders o
            WHERE (
                LOWER(CAST(o.order_status AS CHAR)) IN ('delivered', 'completed')
                OR LOWER(COALESCE(o.shiprocket_status, '')) IN ('7', '23', '26', 'delivered', 'completed', 'fulfilled')
              )
              AND LOWER(CAST(COALESCE(o.order_status, 'pending') AS CHAR)) NOT IN (
                   'cancelled', 'canceled', 'returned', 'refunded',
                   'rto_delivered', 'rto_initiated', 'replacement'
              )
              AND (
                (:sellerPaymentStatus = 'pending'
                  AND (o.seller_payment_status IS NULL OR LOWER(o.seller_payment_status) = 'pending'))
                OR (:sellerPaymentStatus <> 'pending'
                  AND LOWER(COALESCE(o.seller_payment_status, '')) = LOWER(:sellerPaymentStatus))
              )
            """, nativeQuery = true)
    long countDeliveredSellerPaymentsByStatus(@Param("sellerPaymentStatus") String sellerPaymentStatus);

    @Query(value = """
            SELECT COALESCE(SUM(o.total_amount), 0) FROM orders o
            WHERE LOWER(COALESCE(o.seller_payment_status, '')) = 'paid'
              AND (
                LOWER(CAST(o.order_status AS CHAR)) IN ('delivered', 'completed')
                OR LOWER(COALESCE(o.shiprocket_status, '')) IN ('7', '23', '26', 'delivered', 'completed', 'fulfilled')
              )
              AND LOWER(CAST(COALESCE(o.order_status, 'pending') AS CHAR)) NOT IN (
                   'cancelled', 'canceled', 'returned', 'refunded',
                   'rto_delivered', 'rto_initiated', 'replacement'
              )
            """, nativeQuery = true)
    BigDecimal sumPaidDeliveredSellerPaymentAmount();

    @Query(value = """
            SELECT COUNT(*) FROM orders o
            WHERE (
                o.seller_payment_status IS NULL OR LOWER(o.seller_payment_status) = 'pending'
              )
              AND (
                LOWER(CAST(o.order_status AS CHAR)) IN ('delivered', 'completed')
                OR LOWER(COALESCE(o.shiprocket_status, '')) IN ('7', '23', '26', 'delivered', 'completed', 'fulfilled')
              )
              AND LOWER(CAST(COALESCE(o.order_status, 'pending') AS CHAR)) NOT IN (
                   'cancelled', 'canceled', 'returned', 'refunded',
                   'rto_delivered', 'rto_initiated', 'replacement'
              )
              AND DATEDIFF(CURDATE(), DATE(COALESCE(o.shiprocket_synced_at, o.updated_at))) <= :maxDays
            """, nativeQuery = true)
    long countPendingSellerPaymentsWithinDays(@Param("maxDays") int maxDays);

    @Query(value = """
            SELECT COUNT(*) FROM orders o
            WHERE (
                o.seller_payment_status IS NULL OR LOWER(o.seller_payment_status) = 'pending'
              )
              AND (
                LOWER(CAST(o.order_status AS CHAR)) IN ('delivered', 'completed')
                OR LOWER(COALESCE(o.shiprocket_status, '')) IN ('7', '23', '26', 'delivered', 'completed', 'fulfilled')
              )
              AND LOWER(CAST(COALESCE(o.order_status, 'pending') AS CHAR)) NOT IN (
                   'cancelled', 'canceled', 'returned', 'refunded',
                   'rto_delivered', 'rto_initiated', 'replacement'
              )
              AND DATEDIFF(CURDATE(), DATE(COALESCE(o.shiprocket_synced_at, o.updated_at))) BETWEEN :minDays AND :maxDays
            """, nativeQuery = true)
    long countPendingSellerPaymentsDaysBetween(@Param("minDays") int minDays, @Param("maxDays") int maxDays);

    @Query(value = """
            SELECT COUNT(*) FROM orders o
            WHERE (
                o.seller_payment_status IS NULL OR LOWER(o.seller_payment_status) = 'pending'
              )
              AND (
                LOWER(CAST(o.order_status AS CHAR)) IN ('delivered', 'completed')
                OR LOWER(COALESCE(o.shiprocket_status, '')) IN ('7', '23', '26', 'delivered', 'completed', 'fulfilled')
              )
              AND LOWER(CAST(COALESCE(o.order_status, 'pending') AS CHAR)) NOT IN (
                   'cancelled', 'canceled', 'returned', 'refunded',
                   'rto_delivered', 'rto_initiated', 'replacement'
              )
              AND DATEDIFF(CURDATE(), DATE(COALESCE(o.shiprocket_synced_at, o.updated_at))) >= :minDays
            """, nativeQuery = true)
    long countPendingSellerPaymentsAtLeastDays(@Param("minDays") int minDays);

    @Query(value = """
            SELECT COUNT(*) FROM orders o
            WHERE (
                o.seller_payment_status IS NULL OR LOWER(COALESCE(o.seller_payment_status, '')) = 'pending'
              )
              AND LOWER(COALESCE(o.payment_status, '')) IN ('paid', 'success', 'captured', 'completed')
              AND (
                LOWER(CAST(o.order_status AS CHAR)) IN ('delivered', 'completed')
                OR LOWER(COALESCE(o.shiprocket_status, '')) IN ('7', '23', '26', 'delivered', 'completed', 'fulfilled')
              )
              AND LOWER(CAST(COALESCE(o.order_status, 'pending') AS CHAR)) NOT IN (
                   'cancelled', 'canceled', 'returned', 'refunded',
                   'rto_delivered', 'rto_initiated', 'replacement'
              )
              AND DATEDIFF(CURDATE(), DATE(COALESCE(o.shiprocket_synced_at, o.updated_at, o.created_at))) >= :minDays
            """, nativeQuery = true)
    long countOverdueSellerPayoutsAfterCustomerPaid(@Param("minDays") int minDays);

    @Query(value = """
            SELECT * FROM orders o
            WHERE (
                o.seller_payment_status IS NULL OR LOWER(COALESCE(o.seller_payment_status, '')) = 'pending'
              )
              AND LOWER(COALESCE(o.payment_status, '')) IN ('paid', 'success', 'captured', 'completed')
              AND (
                LOWER(CAST(o.order_status AS CHAR)) IN ('delivered', 'completed')
                OR LOWER(COALESCE(o.shiprocket_status, '')) IN ('7', '23', '26', 'delivered', 'completed', 'fulfilled')
              )
              AND LOWER(CAST(COALESCE(o.order_status, 'pending') AS CHAR)) NOT IN (
                   'cancelled', 'canceled', 'returned', 'refunded',
                   'rto_delivered', 'rto_initiated', 'replacement'
              )
              AND DATEDIFF(CURDATE(), DATE(COALESCE(o.shiprocket_synced_at, o.updated_at, o.created_at))) >= :minDays
            ORDER BY COALESCE(o.shiprocket_synced_at, o.updated_at) ASC
            """, nativeQuery = true)
    List<Order> findOverdueSellerPayoutsAfterCustomerPaid(@Param("minDays") int minDays, Pageable pageable);

    @Query("SELECT MIN(o.id) FROM Order o WHERE LOWER(o.shippingEmail) = LOWER(:email)")
    java.util.Optional<Long> findMinIdByShippingEmailIgnoreCase(@Param("email") String email);

    @Query("""
            SELECT o FROM Order o
            WHERE (o.shiprocketShipmentId IS NOT NULL OR o.shiprocketOrderId IS NOT NULL OR o.shiprocketAwbCode IS NOT NULL)
              AND (o.orderStatus IS NULL OR LOWER(o.orderStatus) NOT IN ('delivered', 'completed', 'cancelled', 'returned', 'rto_delivered'))
              AND o.createdAt >= :createdAfter
              AND (o.shiprocketSyncedAt IS NULL OR o.shiprocketSyncedAt <= :syncedBefore)
            ORDER BY o.createdAt DESC
            """)
    List<Order> findShiprocketStatusSyncCandidates(
            @Param("createdAfter") LocalDateTime createdAfter,
            @Param("syncedBefore") LocalDateTime syncedBefore,
            Pageable pageable
    );
}
