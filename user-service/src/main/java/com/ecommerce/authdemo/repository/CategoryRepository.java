package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Main Categories
    List<Category> findByParentIdIsNull();

    // Subcategories
    List<Category> findByParentId(Long parentId);

    // Search Categories
    List<Category> findByCategoryNameContainingIgnoreCase(String keyword);

    List<Category> findTop10ByCategoryNameContainingIgnoreCase(String keyword);
    
    // Count methods for filters
    @Query("SELECT COUNT(p) FROM Product p WHERE p.categoryId = :categoryId AND p.status = 'active'")
    Long countByCategoryId(@Param("categoryId") Long categoryId);
}