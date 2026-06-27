package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Integer> {

    List<Subcategory> findByCategoryIdOrderBySubcategoryNameAsc(Integer categoryId);

    Optional<Subcategory> findBySubcategoryNameIgnoreCaseAndCategoryId(String subcategoryName, Integer categoryId);
}
