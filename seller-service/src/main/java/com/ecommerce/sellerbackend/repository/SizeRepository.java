package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SizeRepository extends JpaRepository<Size, Long> {

    @Query("""
            SELECT s FROM Size s
            WHERE s.sellerId = :sellerId OR s.sellerId IS NULL
            ORDER BY s.createdAt DESC
            """)
    List<Size> findVisibleForSeller(@Param("sellerId") Long sellerId);

    Optional<Size> findByIdAndSellerId(Long id, Long sellerId);

    @Query("""
            SELECT s FROM Size s
            WHERE s.id = :id AND (s.sellerId = :sellerId OR s.sellerId IS NULL)
            """)
    Optional<Size> findEditableByIdForSeller(@Param("id") Long id, @Param("sellerId") Long sellerId);

    @Query("""
            SELECT COUNT(s) > 0 FROM Size s
            WHERE LOWER(s.sizeCode) = LOWER(:code)
              AND (s.sellerId = :sellerId OR s.sellerId IS NULL)
            """)
    boolean existsVisibleCodeForSeller(@Param("sellerId") Long sellerId, @Param("code") String code);

    @Query("""
            SELECT COUNT(s) > 0 FROM Size s
            WHERE LOWER(s.sizeCode) = LOWER(:code)
              AND (s.sellerId = :sellerId OR s.sellerId IS NULL)
              AND s.id <> :id
            """)
    boolean existsVisibleCodeForSellerExcludingId(
            @Param("sellerId") Long sellerId,
            @Param("code") String code,
            @Param("id") Long id);

    @Query("""
            SELECT s FROM Size s
            WHERE (s.sellerId = :sellerId OR s.sellerId IS NULL)
              AND s.status = true
              AND (LOWER(s.sizeName) = LOWER(:value) OR LOWER(s.sizeCode) = LOWER(:value))
            """)
    Optional<Size> findVisibleByNameOrCodeForSeller(
            @Param("sellerId") Long sellerId,
            @Param("value") String value);

    @Query("""
            SELECT s FROM Size s
            WHERE s.id = :id AND (s.sellerId = :sellerId OR s.sellerId IS NULL)
            """)
    Optional<Size> findVisibleByIdForSeller(@Param("id") Long id, @Param("sellerId") Long sellerId);

    @Query("""
            SELECT s FROM Size s
            WHERE (s.sellerId = :sellerId OR s.sellerId IS NULL)
              AND (LOWER(s.sizeName) = LOWER(:value) OR LOWER(s.sizeCode) = LOWER(:value))
            """)
    Optional<Size> findByNameOrCodeForSellerIgnoreStatus(
            @Param("sellerId") Long sellerId,
            @Param("value") String value);
}
