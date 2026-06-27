package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.PushNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PushNotificationRepository extends JpaRepository<PushNotification, Integer> {

    @Query("""
            SELECT p
            FROM PushNotification p
            WHERE (:userId IS NULL OR p.userId = :userId)
              AND (:type IS NULL OR LOWER(p.type) = LOWER(:type))
              AND (:isRead IS NULL OR p.isRead = :isRead)
            ORDER BY p.createdAt DESC
            """)
    List<PushNotification> findWithFilters(@Param("userId") Long userId,
                                           @Param("type") String type,
                                           @Param("isRead") Boolean isRead);

    @Query("""
            SELECT p
            FROM PushNotification p
            WHERE (:userId IS NULL OR p.userId = :userId)
              AND (:type IS NULL OR LOWER(p.type) = LOWER(:type))
              AND (:isRead IS NULL OR p.isRead = :isRead)
            """)
    Page<PushNotification> findPageWithFilters(@Param("userId") Long userId,
                                               @Param("type") String type,
                                               @Param("isRead") Boolean isRead,
                                               Pageable pageable);

    @Modifying
    @Query("""
            UPDATE PushNotification p
            SET p.isRead = TRUE, p.readAt = :readAt
            WHERE p.userId = :userId
              AND (p.isRead = FALSE OR p.isRead IS NULL)
            """)
    int markAllAsReadByUserId(@Param("userId") Long userId, @Param("readAt") java.time.LocalDateTime readAt);

    long countByUserIdAndIsReadFalse(Long userId);

    long countByUserId(Long userId);

    @Query("""
            SELECT COUNT(p) FROM PushNotification p
            WHERE p.userId = :userId
              AND LOWER(p.type) IN ('order', 'orders', 'payment')
            """)
    long countOrdersByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(p) FROM PushNotification p
            WHERE p.userId = :userId
              AND LOWER(p.type) IN ('promotion', 'promotions', 'offer', 'offers')
            """)
    long countPromotionsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(p) FROM PushNotification p
            WHERE p.userId = :userId
              AND LOWER(p.type) IN ('system', 'alert', 'general')
            """)
    long countSystemByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT p FROM PushNotification p
            WHERE p.userId = :userId
              AND (:isRead IS NULL OR p.isRead = :isRead)
              AND (
                    :category IS NULL OR :category = '' OR LOWER(:category) = 'all'
                    OR (LOWER(:category) = 'orders' AND LOWER(p.type) IN ('order', 'orders', 'payment'))
                    OR (LOWER(:category) = 'promotions' AND LOWER(p.type) IN ('promotion', 'promotions', 'offer', 'offers'))
                    OR (LOWER(:category) = 'system' AND LOWER(p.type) IN ('system', 'alert', 'general'))
                  )
            """)
    Page<PushNotification> findInbox(@Param("userId") Long userId,
                                     @Param("category") String category,
                                     @Param("isRead") Boolean isRead,
                                     Pageable pageable);
}
