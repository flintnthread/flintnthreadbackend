package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Integer> {

    List<Subcategory> findByCategoryIdOrderBySubcategoryNameAsc(Integer categoryId);

    List<Subcategory> findBySubcategoryNameContainingIgnoreCase(String name);

    List<Subcategory> findByStatus(Boolean status);

    long count();
}
