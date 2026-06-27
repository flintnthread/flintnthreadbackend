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
