package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Order o SET o.addressId = NULL WHERE o.userId = :userId")
    void clearAddressIdsForUser(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Order o SET o.addressId = NULL WHERE o.addressId = :addressId")
    void clearAddressId(@Param("addressId") Long addressId);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findByUserIdAndOrderStatusOrderByCreatedAtDesc(Long userId, String status);

    @Query(
            value = """
                    SELECT o.id, o.user_id, o.order_number, o.total_amount, o.shipping_amount, o.discount_amount,
                           o.payment_method, o.payment_status, o.order_status, o.created_at,
                           o.razorpay_payment_id, o.shiprocket_awb_code, o.shiprocket_courier_name,
                           o.shiprocket_tracking_url, o.shiprocket_status
                    FROM orders o
                    WHERE o.user_id = :userId
                    ORDER BY o.created_at DESC
                    """,
            nativeQuery = true
    )
    List<Object[]> findSummaryRowsByUserId(@Param("userId") Long userId);

    @Query(
            value = """
                    SELECT o.id, o.user_id, o.order_number, o.total_amount, o.shipping_amount, o.discount_amount,
                           o.payment_method, o.payment_status, o.order_status, o.created_at,
                           o.razorpay_payment_id, o.shiprocket_awb_code, o.shiprocket_courier_name,
                           o.shiprocket_tracking_url, o.shiprocket_status
                    FROM orders o
                    WHERE o.user_id = :userId
                      AND LOWER(COALESCE(o.order_status, '')) = LOWER(:status)
                    ORDER BY o.created_at DESC
                    """,
            nativeQuery = true
    )
    List<Object[]> findSummaryRowsByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") String status
    );

    @Query("""
            SELECT o FROM Order o
            WHERE o.userId = :userId
              AND LOWER(COALESCE(o.orderStatus, '')) = 'cancelled'
            ORDER BY o.createdAt DESC
            """)
    List<Order> findCancelledOrdersByUserId(@Param("userId") Long userId);

    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);
    List<Order> findByRazorpayOrderIdOrderByCreatedAtDesc(String razorpayOrderId);

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByShiprocketAwbCode(String shiprocketAwbCode);

    Optional<Order> findTopByPaymentStatusOrderByCreatedAtDesc(String status);

    Optional<Order> findTopByUserIdAndPaymentStatusOrderByCreatedAtDesc(Long userId, String paymentStatus);

    long countByUserId(Long userId);

    long countByUserIdAndPaymentStatus(Long userId, String paymentStatus);

    long countByUserIdAndDiscountAmountGreaterThan(Long userId, Double minDiscountAmount);

    boolean existsByUserIdAndReferralInviterDiscountAppliedTrue(Long userId);

    @Modifying
    @Transactional
    @Query("""
            UPDATE Order o
            SET o.shiprocketAwbCode = COALESCE(:awb, o.shiprocketAwbCode),
                o.shiprocketCourierName = COALESCE(:courierName, o.shiprocketCourierName),
                o.shiprocketTrackingUrl = COALESCE(:trackingUrl, o.shiprocketTrackingUrl),
                o.shiprocketStatus = :status,
                o.shiprocketSyncedAt = CURRENT_TIMESTAMP
            WHERE o.orderNumber = :orderNumber
            """)
    void updateShipment(
            @Param("orderNumber") String orderNumber,
            @Param("awb") String awb,
            @Param("courierName") String courierName,
            @Param("trackingUrl") String trackingUrl,
            @Param("status") String status
    );

    @Modifying
    @Transactional
    @Query("""

UPDATE Order o

SET
o.shiprocketStatus = :status,
o.orderStatus = :status

WHERE o.shiprocketAwbCode = :awb

""")
    void updateOrderStatusFromWebhook(

            @Param("awb")
            String awb,

            @Param("status")
            String status
    );

}