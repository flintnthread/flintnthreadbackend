package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.ads.AdsUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AdsUserRepository extends JpaRepository<AdsUser, Integer> {

    @Query("""
            SELECT u FROM AdsUser u
            WHERE (:search IS NULL OR :search = '' OR
                   LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(u.company, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY u.id DESC
            """)
    Page<AdsUser> search(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(u) FROM AdsUser u WHERE u.createdAt >= :from AND u.createdAt < :to")
    long countCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
