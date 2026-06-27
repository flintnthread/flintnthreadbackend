package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.Pincode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PincodeRepository extends JpaRepository<Pincode, Integer> {

    @Query("""
            SELECT p
            FROM Pincode p
            WHERE (:countryId IS NULL OR p.countryId = :countryId)
              AND (:stateId IS NULL OR p.stateId = :stateId)
              AND (:cityId IS NULL OR p.cityId = :cityId)
              AND (:areaId IS NULL OR p.areaId = :areaId)
              AND (:status IS NULL OR p.status = :status)
              AND (:code IS NULL OR p.pincode = :code)
            ORDER BY p.id DESC
            """)
    List<Pincode> findWithFilters(@Param("countryId") Integer countryId,
                                  @Param("stateId") Integer stateId,
                                  @Param("cityId") Integer cityId,
                                  @Param("areaId") Integer areaId,
                                  @Param("status") Boolean status,
                                  @Param("code") String code);

    List<Pincode> findByPincode(String pincode);
}
