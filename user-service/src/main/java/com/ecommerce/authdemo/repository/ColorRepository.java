package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ColorRepository extends JpaRepository<Color, Long> {

    List<Color> findAll();

    Color findByName(String name);

    List<Color> findByStatus(Integer status);

    @Query("SELECT c FROM Color c WHERE LOWER(TRIM(c.name)) IN :names")
    List<Color> findByLowerNamesIn(@Param("names") Collection<String> names);
}
