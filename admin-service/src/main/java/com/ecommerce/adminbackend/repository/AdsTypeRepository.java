package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.ads.AdsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdsTypeRepository extends JpaRepository<AdsType, Integer> {

    @Query("""
            SELECT t FROM AdsType t
            WHERE (:search IS NULL OR :search = '' OR
                   LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(t.category) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR :status = '' OR LOWER(t.status) = LOWER(:status))
            ORDER BY t.id DESC
            """)
    List<AdsType> search(@Param("search") String search, @Param("status") String status);
}
