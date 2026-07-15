package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.DeliveryPincode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryPincodeRepository extends JpaRepository<DeliveryPincode, Integer> {

    /** Status column is boolean (true = active), matching the `pincodes` table / location API. */
    boolean existsByPincodeAndStatus(String pincode, Boolean status);

    /**
     * Serviceable when the pincode exists and is not explicitly disabled.
     * Treats null status as active (legacy rows).
     */
    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END
            FROM DeliveryPincode p
            WHERE p.pincode = :pincode
              AND (p.status IS NULL OR p.status = TRUE)
            """)
    boolean existsServiceablePincode(@Param("pincode") String pincode);
}
