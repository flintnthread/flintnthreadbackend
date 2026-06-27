package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

    public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

        @Query("SELECT pv FROM ProductVariant pv LEFT JOIN FETCH pv.product WHERE pv.id = :id")
        Optional<ProductVariant> findByIdWithProduct(@Param("id") Long id);

        List<ProductVariant> findByProductId(Long productId);

        @Query("SELECT DISTINCT pv.size FROM ProductVariant pv WHERE pv.size IS NOT NULL AND pv.size != ''")
        List<String> findAllDistinctSizes();

        @Query("SELECT DISTINCT pv.color FROM ProductVariant pv WHERE pv.color IS NOT NULL AND pv.color != ''")
        List<String> findAllDistinctColors();

        @Query("SELECT DISTINCT pv.size FROM ProductVariant pv WHERE pv.product.id = :productId AND pv.size IS NOT NULL AND pv.size != ''")
        List<String> findDistinctSizesByProductId(Long productId);

        @Query("SELECT DISTINCT pv.color FROM ProductVariant pv WHERE pv.product.id = :productId AND pv.color IS NOT NULL AND pv.color != ''")
        List<String> findDistinctColorsByProductId(Long productId);

        @Query("SELECT pv.stock FROM ProductVariant pv WHERE pv.id = :variantId")
        Optional<Integer> findStockByVariantId(@Param("variantId") Long variantId);

        @Modifying
        @Query("""
                UPDATE ProductVariant pv
                SET pv.stock = pv.stock - :qty
                WHERE pv.id = :variantId
                  AND pv.stock IS NOT NULL
                  AND pv.stock >= :qty
                """)
        int decrementStockIfAvailable(@Param("variantId") Long variantId, @Param("qty") int qty);
    }

