package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CityRepository extends JpaRepository<City, Integer> {

    @Query("""
            SELECT c
            FROM City c
            WHERE (:countryId IS NULL OR c.countryId = :countryId)
              AND (:stateId IS NULL OR c.stateId = :stateId)
              AND (:status IS NULL OR c.status = :status)
            ORDER BY c.cityName ASC
            """)
    List<City> findWithFilters(@Param("countryId") Integer countryId,
                               @Param("stateId") Integer stateId,
                               @Param("status") Boolean status);
}
