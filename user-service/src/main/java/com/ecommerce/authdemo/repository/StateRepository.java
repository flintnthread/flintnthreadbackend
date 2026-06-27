package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StateRepository extends JpaRepository<State, Integer> {

    @Query("""
            SELECT s
            FROM State s
            WHERE (:countryId IS NULL OR s.countryId = :countryId)
              AND (:status IS NULL OR s.status = :status)
            ORDER BY s.stateName ASC
            """)
    List<State> findWithFilters(@Param("countryId") Integer countryId,
                                @Param("status") Boolean status);
}
