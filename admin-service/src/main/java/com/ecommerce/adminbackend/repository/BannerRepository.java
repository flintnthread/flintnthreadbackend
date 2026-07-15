package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.cms.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Integer> {

    @Query("""
            SELECT b FROM Banner b
            WHERE (:search IS NULL OR :search = '' OR
                   LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(b.textContent, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR b.status = :status)
            ORDER BY b.id DESC
            """)
    List<Banner> search(@Param("search") String search, @Param("status") Integer status);

    List<Banner> findAllByOrderByIdDesc();
}
