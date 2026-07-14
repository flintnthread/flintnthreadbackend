package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByStatusIgnoreCase(String status, Pageable pageable);

    long countByStatusIgnoreCase(String status);

    @Query("SELECT COUNT(p) FROM Product p WHERE LOWER(p.status) IN ('approved', 'active')")
    long countApproved();

    @Query("SELECT COUNT(p) FROM Product p WHERE LOWER(p.status) = 'rejected'")
    long countRejected();

    @Query("SELECT COUNT(p) FROM Product p WHERE LOWER(p.status) IN ('inactive', 'rejected')")
    long countInactiveProducts();

    @Query("""
            SELECT p FROM Product p
            WHERE (:status IS NULL OR :status = '' OR LOWER(p.status) = LOWER(:status))
              AND (:search IS NULL OR :search = '' OR
                   LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(p.sku, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:sellerId IS NULL OR p.sellerId = :sellerId)
              AND (:adminOnly IS NULL OR :adminOnly = false OR p.sellerId IS NULL)
              AND (:categoryId IS NULL OR p.categoryId = :categoryId)
              AND (:subcategoryId IS NULL OR p.subcategoryId = :subcategoryId)
              AND (:mainCategoryId IS NULL OR p.categoryId = :mainCategoryId OR p.categoryId IN (
                   SELECT c.id FROM Category c WHERE c.parentId = :mainCategoryId))
            ORDER BY p.updatedAt DESC, p.createdAt DESC
            """)
    Page<Product> searchProducts(@Param("status") String status,
                                 @Param("search") String search,
                                 @Param("sellerId") Long sellerId,
                                 @Param("adminOnly") Boolean adminOnly,
                                 @Param("mainCategoryId") Integer mainCategoryId,
                                 @Param("categoryId") Integer categoryId,
                                 @Param("subcategoryId") Integer subcategoryId,
                                 Pageable pageable);

    long countBySellerIdIsNull();

    long countBySellerId(Long sellerId);

    List<Product> findBySellerId(Long sellerId);

    @Query("""
            SELECT LOWER(p.status), COUNT(p)
            FROM Product p
            WHERE p.sellerId = :sellerId
            GROUP BY LOWER(p.status)
            """)
    List<Object[]> countProductsByStatusForSeller(@Param("sellerId") Long sellerId);

    @Query("""
            SELECT p.sellerId, COUNT(p)
            FROM Product p
            WHERE p.sellerId IN :sellerIds
            GROUP BY p.sellerId
            """)
    List<Object[]> countProductsBySellerIds(@Param("sellerIds") Collection<Long> sellerIds);

    @Query("""
            SELECT COUNT(p) FROM Product p
            WHERE (:sellerId IS NULL OR p.sellerId = :sellerId)
              AND p.createdAt <= :endDate
            """)
    long countCreatedOnOrBefore(@Param("sellerId") Long sellerId, @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COUNT(p) FROM Product p
            WHERE (:sellerId IS NULL OR p.sellerId = :sellerId)
              AND p.createdAt >= :startDate
              AND p.createdAt <= :endDate
            """)
    long countCreatedInPeriod(@Param("sellerId") Long sellerId,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);
}
