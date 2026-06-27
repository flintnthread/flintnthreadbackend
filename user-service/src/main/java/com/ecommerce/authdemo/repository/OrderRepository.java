package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Modifying
    @Query("UPDATE Order o SET o.addressId = NULL WHERE o.userId = :userId")
    void clearAddressIdsForUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Order o SET o.addressId = NULL WHERE o.addressId = :addressId")
    void clearAddressId(@Param("addressId") Long addressId);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findByUserIdAndOrderStatusOrderByCreatedAtDesc(Long userId, String status);

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

SET
o.shiprocketAwbCode = :awb,
o.shiprocketCourierName = :courierName,
o.shiprocketTrackingUrl = :trackingUrl,
o.shiprocketStatus = :status,
o.orderStatus = :status

WHERE o.orderNumber = :orderNumber

""")
    void updateShipment(

            @Param("orderNumber")
            String orderNumber,

            @Param("awb")
            String awb,

            @Param("courierName")
            String courierName,

            @Param("trackingUrl")
            String trackingUrl,

            @Param("status")
            String status
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