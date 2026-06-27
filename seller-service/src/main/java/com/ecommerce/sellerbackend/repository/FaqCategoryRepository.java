package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.FaqCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqCategoryRepository extends JpaRepository<FaqCategory, Integer> {

    List<FaqCategory> findByStatusTrueOrderBySortOrderAscIdAsc();
}
