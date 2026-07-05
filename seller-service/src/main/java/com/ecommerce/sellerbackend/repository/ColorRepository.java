package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ColorRepository extends JpaRepository<Color, Long> {

    @Query("""
            SELECT c FROM Color c
            WHERE c.sellerId = :sellerId OR c.sellerId IS NULL
            ORDER BY c.createdAt DESC
            """)
    List<Color> findVisibleForSeller(@Param("sellerId") Long sellerId);

    Optional<Color> findByIdAndSellerId(Long id, Long sellerId);

    @Query("""
            SELECT COUNT(c) > 0 FROM Color c
            WHERE LOWER(c.colorCode) = LOWER(:code)
              AND (c.sellerId = :sellerId OR c.sellerId IS NULL)
            """)
    boolean existsVisibleCodeForSeller(@Param("sellerId") Long sellerId, @Param("code") String code);

    @Query("""
            SELECT COUNT(c) > 0 FROM Color c
            WHERE LOWER(c.colorCode) = LOWER(:code)
              AND (c.sellerId = :sellerId OR c.sellerId IS NULL)
              AND c.id <> :id
            """)
    boolean existsVisibleCodeForSellerExcludingId(
            @Param("sellerId") Long sellerId,
            @Param("code") String code,
            @Param("id") Long id);

    @Query("""
            SELECT c FROM Color c
            WHERE (c.sellerId = :sellerId OR c.sellerId IS NULL)
              AND LOWER(c.colorName) = LOWER(:name)
              AND c.status = true
            """)
    Optional<Color> findVisibleByNameForSeller(@Param("sellerId") Long sellerId, @Param("name") String name);

    @Query("""
            SELECT c FROM Color c
            WHERE c.id = :id AND (c.sellerId = :sellerId OR c.sellerId IS NULL)
            """)
    Optional<Color> findVisibleByIdForSeller(@Param("id") Long id, @Param("sellerId") Long sellerId);
}
