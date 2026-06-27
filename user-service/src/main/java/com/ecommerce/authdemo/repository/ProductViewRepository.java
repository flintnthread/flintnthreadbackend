package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ProductView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

    public interface ProductViewRepository extends JpaRepository<ProductView, Long> {

        List<ProductView> findTop10ByUserIdOrderByViewedAtDesc(Long userId);

        List<ProductView> findTop10BySessionIdOrderByViewedAtDesc(String sessionId);

        @Query(value = """
            SELECT product_id
            FROM product_views
            GROUP BY product_id
            ORDER BY COUNT(*) DESC
            LIMIT 10
            """, nativeQuery = true)
        List<Long> findMostViewedProductIds();

        @Query(value = """
                SELECT pv.product_id AS productId, MAX(pv.viewed_at) AS viewedAt
                FROM product_views pv
                WHERE (
                    (:userId IS NOT NULL AND pv.user_id = :userId)
                    OR (:sessionId IS NOT NULL AND :sessionId <> '' AND pv.session_id = :sessionId)
                )
                GROUP BY pv.product_id
                ORDER BY viewedAt DESC
                LIMIT 50
                """, nativeQuery = true)
        List<Object[]> findRecentlyViewedSummaries(
                @Param("userId") Long userId,
                @Param("sessionId") String sessionId
        );
    }

