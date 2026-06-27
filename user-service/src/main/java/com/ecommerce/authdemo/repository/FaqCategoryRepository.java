package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.FaqCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FaqCategoryRepository extends JpaRepository<FaqCategory, Integer> {

    @Query("""
            SELECT f
            FROM FaqCategory f
            WHERE (:status IS NULL OR f.status = :status)
            ORDER BY f.sortOrder ASC, f.id ASC
            """)
    List<FaqCategory> findWithStatus(@Param("status") Boolean status);
}
