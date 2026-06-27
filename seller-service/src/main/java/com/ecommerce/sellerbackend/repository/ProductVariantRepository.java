package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdIn(Collection<Long> productIds);

    List<ProductVariant> findByProductIdOrderByIdAsc(Long productId);

    void deleteByProductId(Long productId);

    java.util.Optional<ProductVariant> findByIdAndProductId(Long id, Long productId);

    @Query("""
            SELECT MIN(COALESCE(v.finalPrice, v.sellingPrice))
            FROM ProductVariant v
            JOIN Product p ON p.id = v.productId
            WHERE p.sellerId = :sellerId
              AND COALESCE(v.finalPrice, v.sellingPrice) IS NOT NULL
            """)
    Optional<BigDecimal> findMinPriceForSeller(@Param("sellerId") Long sellerId);

    @Query("""
            SELECT MAX(COALESCE(v.finalPrice, v.sellingPrice))
            FROM ProductVariant v
            JOIN Product p ON p.id = v.productId
            WHERE p.sellerId = :sellerId
              AND COALESCE(v.finalPrice, v.sellingPrice) IS NOT NULL
            """)
    Optional<BigDecimal> findMaxPriceForSeller(@Param("sellerId") Long sellerId);
}
