package com.ecommerce.authdemo.repository;





import com.ecommerce.authdemo.entity.RazorpayConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

    public interface RazorpayConfigRepository extends JpaRepository<RazorpayConfig, Integer> {

        Optional<RazorpayConfig> findTopByOrderByIdDesc(); // latest config
    }

