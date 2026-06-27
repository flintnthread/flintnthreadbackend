package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, Integer status);

    @Query("""
            SELECT r FROM ProductReview r
            JOIN Product p ON p.id = r.productId
            WHERE p.sellerId = :sellerId AND (r.status IS NULL OR r.status = 1)
            ORDER BY r.createdAt DESC
            """)
    List<ProductReview> findActiveForSeller(@Param("sellerId") Long sellerId);

    @Query("""
            SELECT AVG(r.rating) FROM ProductReview r
            JOIN Product p ON p.id = r.productId
            WHERE p.sellerId = :sellerId AND (r.status IS NULL OR r.status = 1)
            """)
    Double averageRatingForSeller(@Param("sellerId") Long sellerId);

    @Query("""
            SELECT COUNT(r) FROM ProductReview r
            JOIN Product p ON p.id = r.productId
            WHERE p.sellerId = :sellerId AND (r.status IS NULL OR r.status = 1)
            """)
    long countActiveForSeller(@Param("sellerId") Long sellerId);

    @Query("""
            SELECT r.productId, AVG(r.rating) FROM ProductReview r
            WHERE r.productId IN :productIds AND (r.status IS NULL OR r.status = 1)
            GROUP BY r.productId
            """)
    List<Object[]> averageRatingByProductIds(@Param("productIds") Collection<Long> productIds);
}
