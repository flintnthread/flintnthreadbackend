package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    @Query("""
            SELECT p.id, p.name, COALESCE(SUM(pv.stock), 0)
            FROM Product p
            LEFT JOIN ProductVariant pv ON pv.productId = p.id
            WHERE p.sellerId = :sellerId
            GROUP BY p.id, p.name
            HAVING COALESCE(SUM(pv.stock), 0) > 0
            """)
    List<Object[]> findInStockProductsWithStockCount(@Param("sellerId") Long sellerId);

    long countBySellerId(Long sellerId);

    Optional<Product> findByIdAndSellerId(Long id, Long sellerId);

    @Query("""
            SELECT COUNT(p) FROM Product p
            WHERE LOWER(p.status) IN ('approved', 'active')
            """)
    long countApprovedProducts();

    @Query(
            value = """
                    SELECT id, name
                    FROM products
                    WHERE id IN (:ids)
                      AND name IS NOT NULL
                      AND TRIM(name) <> ''
                      AND LOWER(TRIM(name)) <> 'product'
                    """,
            nativeQuery = true)
    List<Object[]> findNamesByProductIds(@Param("ids") Collection<Long> ids);
}
