package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ProductPincode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductPincodeRepository extends JpaRepository<ProductPincode, Long> {

    @Query(
            value =
                    """
                    SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END
                    FROM product_pincodes pp
                    INNER JOIN pincodes dp ON dp.id = pp.pincode_id
                    WHERE pp.product_id = :productId
                      AND dp.pincode = :pincode
                      AND (pp.status IS NULL OR pp.status = 1)
                      AND (dp.status IS NULL OR dp.status = 1)
                    """,
            nativeQuery = true
    )
    int countActiveDeliveryForProductAndPincode(
            @Param("productId") Long productId,
            @Param("pincode") String pincode
    );

    default boolean existsActiveDeliveryForProductAndPincode(Long productId, String pincode) {
        return countActiveDeliveryForProductAndPincode(productId, pincode) > 0;
    }
}
