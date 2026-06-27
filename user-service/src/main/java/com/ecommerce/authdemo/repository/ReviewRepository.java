package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ProductReview, Long> {
    List<ProductReview> findByProduct_IdOrderByCreatedAtDesc(Long productId);

    List<ProductReview> findByProduct_IdAndStatusTrueOrderByCreatedAtDesc(Long productId);

    Page<ProductReview> findByProduct_IdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    Page<ProductReview> findByProduct_IdAndStatusTrueOrderByCreatedAtDesc(Long productId, Pageable pageable);

    @Query("""
            SELECT r FROM ProductReview r
            WHERE r.product.sellerId = :sellerId AND r.status = true
            ORDER BY r.createdAt DESC
            """)
    Page<ProductReview> findActiveBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    @Query("""
            SELECT COUNT(r) FROM ProductReview r
            WHERE r.product.sellerId = :sellerId AND r.status = true
            """)
    long countActiveBySellerId(@Param("sellerId") Long sellerId);

    @Query("""
            SELECT AVG(r.rating) FROM ProductReview r
            WHERE r.product.sellerId = :sellerId AND r.status = true
            """)
    Double averageRatingBySellerId(@Param("sellerId") Long sellerId);

    List<ProductReview> findByUserIdAndStatusTrueOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndStatusTrue(Long userId);
}
