package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.DeliveryCharges;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DeliveryChargesRepository extends JpaRepository<DeliveryCharges, Integer> {

    @Query("""
            SELECT d
            FROM DeliveryCharges d
            WHERE d.status = true
              AND d.weightMin <= :weight
              AND d.weightMax >= :weight
            ORDER BY d.weightMin DESC, d.weightMax ASC, d.id ASC
            """)
    List<DeliveryCharges> findActiveByWeight(@Param("weight") BigDecimal weight);

    @Query("""
            SELECT d
            FROM DeliveryCharges d
            WHERE d.id <> :excludeId
              AND d.weightMin <= :weightMax
              AND d.weightMax >= :weightMin
            """)
    List<DeliveryCharges> findOverlappingSlabs(
            @Param("weightMin") BigDecimal weightMin,
            @Param("weightMax") BigDecimal weightMax,
            @Param("excludeId") Integer excludeId);

    @Query("""
            SELECT d
            FROM DeliveryCharges d
            WHERE (:status IS NULL OR d.status = :status)
            ORDER BY d.weightMin ASC
            """)
    List<DeliveryCharges> findWithStatus(@Param("status") Boolean status);
}


