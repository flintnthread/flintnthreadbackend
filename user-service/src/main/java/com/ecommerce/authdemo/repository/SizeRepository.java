package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SizeRepository extends JpaRepository<Size, Long> {

    List<Size> findAll();

    Size findByName(String name);

    List<Size> findByStatus(Integer status);

    @Query("SELECT s FROM Size s WHERE LOWER(TRIM(s.name)) IN :names")
    List<Size> findByLowerNamesIn(@Param("names") Collection<String> names);
}
