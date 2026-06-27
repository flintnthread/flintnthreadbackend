package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.FaqCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqCategoryRepository extends JpaRepository<FaqCategory, Integer> {

    List<FaqCategory> findAllByOrderBySortOrderAscIdAsc();
}
