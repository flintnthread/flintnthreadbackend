package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    Optional<Category> findByCategoryNameIgnoreCase(String categoryName);
}
