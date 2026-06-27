package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.ProductView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ProductViewRepository extends JpaRepository<ProductView, Long> {

    @Query("""
            SELECT COUNT(v) FROM ProductView v
            JOIN Product p ON p.id = v.productId
            WHERE p.sellerId = :sellerId
            """)
    long countViewsForSeller(@Param("sellerId") Long sellerId);

    @Query("""
            SELECT COUNT(v) FROM ProductView v
            JOIN Product p ON p.id = v.productId
            WHERE p.sellerId = :sellerId
              AND v.viewedAt >= :from
              AND v.viewedAt < :to
            """)
    long countViewsForSellerBetween(
            @Param("sellerId") Long sellerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
