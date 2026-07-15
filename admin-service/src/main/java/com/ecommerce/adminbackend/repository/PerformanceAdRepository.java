package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.ads.PerformanceAd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PerformanceAdRepository extends JpaRepository<PerformanceAd, Integer> {

    @Query("""
            SELECT p FROM PerformanceAd p
            WHERE (:search IS NULL OR :search = '' OR
                   LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(p.type) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR :status = '' OR LOWER(p.status) = LOWER(:status))
            ORDER BY p.id DESC
            """)
    List<PerformanceAd> search(@Param("search") String search, @Param("status") String status);
}
