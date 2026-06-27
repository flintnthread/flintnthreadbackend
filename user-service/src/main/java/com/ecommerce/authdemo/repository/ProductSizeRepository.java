package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ProductSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductSizeRepository extends JpaRepository<ProductSize, Long> {
    
    @Query("SELECT DISTINCT s.name FROM ProductSize ps JOIN ps.size s WHERE ps.product.id = :productId")
    List<String> findSizeNamesByProductId(@Param("productId") Long productId);
    
    @Query("SELECT COUNT(ps) FROM ProductSize ps WHERE ps.size.id = :sizeId")
    Long countBySizeId(@Param("sizeId") Long sizeId);
}
