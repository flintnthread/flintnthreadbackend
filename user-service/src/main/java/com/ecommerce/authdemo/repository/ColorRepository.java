package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ColorRepository extends JpaRepository<Color, Long> {
    
    List<Color> findAll();
    
    Color findByName(String name);
    
    List<Color> findByStatus(Integer status);
}
