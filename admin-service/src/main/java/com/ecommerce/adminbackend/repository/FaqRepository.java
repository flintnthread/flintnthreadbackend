package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Integer> {

    List<Faq> findByCategoryIdOrderBySortOrderAscIdAsc(Integer categoryId);

    void deleteByCategoryId(Integer categoryId);

    long countByCategoryId(Integer categoryId);
}
