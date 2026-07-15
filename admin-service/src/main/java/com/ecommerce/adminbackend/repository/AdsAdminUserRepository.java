package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.ads.AdsAdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdsAdminUserRepository extends JpaRepository<AdsAdminUser, Integer> {

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    Optional<AdsAdminUser> findByUsernameIgnoreCase(String username);

    @Query("""
            SELECT u FROM AdsAdminUser u
            WHERE (:search IS NULL OR :search = '' OR
                   LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR :status = '' OR LOWER(u.status) = LOWER(:status))
            ORDER BY u.id DESC
            """)
    List<AdsAdminUser> search(@Param("search") String search, @Param("status") String status);
}
