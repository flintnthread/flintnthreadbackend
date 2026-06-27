package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SizeRepository extends JpaRepository<Size, Long> {
    
    List<Size> findAll();
    
    Size findByName(String name);
    
    List<Size> findByStatus(Integer status);
}
