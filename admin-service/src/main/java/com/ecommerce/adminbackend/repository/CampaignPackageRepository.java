package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.ads.CampaignPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CampaignPackageRepository extends JpaRepository<CampaignPackage, Integer> {

    @Query("""
            SELECT c FROM CampaignPackage c
            WHERE (:search IS NULL OR :search = '' OR
                   LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(c.type) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR :status = '' OR LOWER(c.status) = LOWER(:status))
            ORDER BY c.id DESC
            """)
    List<CampaignPackage> search(@Param("search") String search, @Param("status") String status);
}
