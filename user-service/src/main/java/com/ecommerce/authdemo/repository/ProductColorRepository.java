package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ProductColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductColorRepository extends JpaRepository<ProductColor, Long> {
    
    @Query("SELECT DISTINCT c.name FROM ProductColor pc JOIN pc.color c WHERE pc.product.id = :productId")
    List<String> findColorNamesByProductId(@Param("productId") Long productId);
    
    @Query("SELECT COUNT(pc) FROM ProductColor pc WHERE pc.color.id = :colorId")
    Long countByColorId(@Param("colorId") Long colorId);
}
