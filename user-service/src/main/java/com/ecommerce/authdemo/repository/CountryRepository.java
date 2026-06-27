package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CountryRepository extends JpaRepository<Country, Integer> {

    @Query("""
            SELECT c
            FROM Country c
            WHERE (:status IS NULL OR c.status = :status)
            ORDER BY c.countryName ASC
            """)
    List<Country> findWithStatus(@Param("status") Boolean status);
}
