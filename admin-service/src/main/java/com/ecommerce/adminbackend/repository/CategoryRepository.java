package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // Find main categories (parent_id is null)
    List<Category> findByParentIdIsNullOrderByCategoryNameAsc();

    // Find subcategories by parent_id
    List<Category> findByParentIdOrderByCategoryNameAsc(Integer parentId);

    // Search categories by name
    @Query("SELECT c FROM Category c WHERE LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY c.categoryName ASC")
    List<Category> searchByName(@Param("query") String query);

    // Find by status (Boolean)
    List<Category> findByStatus(Boolean status);

    // Count main categories
    long countByParentIdIsNull();

    // Count subcategories by parent
    long countByParentId(Integer parentId);
}
