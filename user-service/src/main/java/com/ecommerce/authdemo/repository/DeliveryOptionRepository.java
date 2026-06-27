package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.DeliveryOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeliveryOptionRepository extends JpaRepository<DeliveryOption, Integer> {

    @Query("""
            SELECT d
            FROM DeliveryOption d
            WHERE (:sellerId IS NULL OR d.sellerId = :sellerId)
              AND (:isActive IS NULL OR d.isActive = :isActive)
            ORDER BY d.id DESC
            """)
    List<DeliveryOption> findWithFilters(@Param("sellerId") Integer sellerId,
                                         @Param("isActive") Boolean isActive);
}
