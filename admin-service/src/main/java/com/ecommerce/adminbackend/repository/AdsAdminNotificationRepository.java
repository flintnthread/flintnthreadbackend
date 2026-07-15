package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.ads.AdsAdminNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdsAdminNotificationRepository extends JpaRepository<AdsAdminNotification, Integer> {

    @Query("""
            SELECT n FROM AdsAdminNotification n
            WHERE (:search IS NULL OR :search = '' OR
                   LOWER(n.orderId) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(n.userName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(n.userEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(n.adName) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR :status = '' OR LOWER(n.status) = LOWER(:status))
              AND (:unreadOnly IS NULL OR :unreadOnly = false OR n.isRead = false OR n.isRead IS NULL)
            ORDER BY n.id DESC
            """)
    Page<AdsAdminNotification> search(
            @Param("search") String search,
            @Param("status") String status,
            @Param("unreadOnly") Boolean unreadOnly,
            Pageable pageable);

    long countByIsReadFalseOrIsReadIsNull();

    long countByIsReadTrue();
}
