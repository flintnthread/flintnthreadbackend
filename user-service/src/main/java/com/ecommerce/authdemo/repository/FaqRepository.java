package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Integer> {

    @Query("""
            SELECT f
            FROM Faq f
            WHERE (:categoryId IS NULL OR f.categoryId = :categoryId)
              AND (:status IS NULL OR f.status = :status)
            ORDER BY f.sortOrder ASC, f.id ASC
            """)
    List<Faq> findWithFilters(@Param("categoryId") Integer categoryId,
                              @Param("status") Boolean status);
}
