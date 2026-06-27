package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.TermsConditions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TermsConditionsRepository extends JpaRepository<TermsConditions, Integer> {
    Optional<TermsConditions> findTopByOrderByIdDesc();
}
