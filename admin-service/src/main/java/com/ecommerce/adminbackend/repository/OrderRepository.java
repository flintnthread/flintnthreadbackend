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
            WHERE (:status IS NULL OR :status = '' OR LOWER(o.orderStatus) = LOWER(:status))
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

    @Query("""
            SELECT o FROM Order o
            WHERE o.sellerPaymentStatus IS NOT NULL
              AND (
                :status IS NULL OR :status = ''
                OR (LOWER(:status) = 'paid-cancelled' AND LOWER(o.sellerPaymentStatus) IN ('paid', 'cancelled'))
                OR (LOWER(:status) <> 'paid-cancelled' AND LOWER(o.sellerPaymentStatus) = LOWER(:status))
              )
            ORDER BY
              CASE
                WHEN LOWER(o.sellerPaymentStatus) = 'pending' THEN 0
                WHEN LOWER(o.sellerPaymentStatus) = 'paid' THEN 1
                ELSE 2
              END,
              o.updatedAt ASC,
              o.id DESC
            """)
    Page<Order> findSellerPayments(@Param("status") String status, Pageable pageable);

    long countBySellerPaymentStatusIgnoreCase(String sellerPaymentStatus);

    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o
            WHERE LOWER(o.sellerPaymentStatus) = 'paid'
            """)
    BigDecimal sumPaidSellerPaymentAmount();

    @Query(value = """
            SELECT COUNT(*) FROM orders o
            WHERE LOWER(o.seller_payment_status) = 'pending'
              AND DATEDIFF(CURDATE(), DATE(o.updated_at)) <= :maxDays
            """, nativeQuery = true)
    long countPendingSellerPaymentsWithinDays(@Param("maxDays") int maxDays);

    @Query(value = """
            SELECT COUNT(*) FROM orders o
            WHERE LOWER(o.seller_payment_status) = 'pending'
              AND DATEDIFF(CURDATE(), DATE(o.updated_at)) BETWEEN :minDays AND :maxDays
            """, nativeQuery = true)
    long countPendingSellerPaymentsDaysBetween(@Param("minDays") int minDays, @Param("maxDays") int maxDays);

    @Query(value = """
            SELECT COUNT(*) FROM orders o
            WHERE LOWER(o.seller_payment_status) = 'pending'
              AND DATEDIFF(CURDATE(), DATE(o.updated_at)) >= :minDays
            """, nativeQuery = true)
    long countPendingSellerPaymentsAtLeastDays(@Param("minDays") int minDays);

    @Query(value = """
            SELECT COUNT(*) FROM orders o
            WHERE LOWER(COALESCE(o.seller_payment_status, '')) = 'pending'
              AND LOWER(COALESCE(o.payment_status, '')) IN ('paid', 'success', 'captured', 'completed')
              AND DATEDIFF(CURDATE(), DATE(COALESCE(o.created_at, o.updated_at))) >= :minDays
            """, nativeQuery = true)
    long countOverdueSellerPayoutsAfterCustomerPaid(@Param("minDays") int minDays);

    @Query(value = """
            SELECT * FROM orders o
            WHERE LOWER(COALESCE(o.seller_payment_status, '')) = 'pending'
              AND LOWER(COALESCE(o.payment_status, '')) IN ('paid', 'success', 'captured', 'completed')
              AND DATEDIFF(CURDATE(), DATE(COALESCE(o.created_at, o.updated_at))) >= :minDays
            ORDER BY o.created_at ASC
            """, nativeQuery = true)
    List<Order> findOverdueSellerPayoutsAfterCustomerPaid(@Param("minDays") int minDays, Pageable pageable);

    @Query("SELECT MIN(o.id) FROM Order o WHERE LOWER(o.shippingEmail) = LOWER(:email)")
    java.util.Optional<Long> findMinIdByShippingEmailIgnoreCase(@Param("email") String email);
}
