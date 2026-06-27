package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.Pincode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PincodeRepository extends JpaRepository<Pincode, Integer> {

    @Query(value = """
            SELECT p.id, p.pincode, a.area_name, c.city_name, s.state_name, co.country_name
            FROM pincodes p
            JOIN areas a ON a.id = p.area_id
            JOIN cities c ON c.id = a.city_id
            JOIN states s ON s.id = c.state_id
            JOIN countries co ON co.id = s.country_id
            WHERE (:q IS NULL OR :q = '' OR p.pincode LIKE CONCAT('%', :q, '%')
                OR LOWER(a.area_name) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.city_name) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY p.pincode, a.area_name
            LIMIT 500
            """, nativeQuery = true)
    List<Object[]> searchWithLocation(@Param("q") String query);
}
