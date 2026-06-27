package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ProductEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductEmbeddingRepository extends JpaRepository<ProductEmbedding, Long> {

    Optional<ProductEmbedding> findByProductIdAndIsActive(Long productId, Boolean isActive);

    List<ProductEmbedding> findByIsActive(Boolean isActive);

    @Query("SELECT pe.product.id FROM ProductEmbedding pe WHERE pe.isActive = true")
    List<Long> findAllProductIdsWithEmbeddings();

    @Query("SELECT pe FROM ProductEmbedding pe WHERE pe.product.id IN :productIds AND pe.isActive = true")
    List<ProductEmbedding> findByProductIdsAndIsActive(@Param("productIds") List<Long> productIds);
}
