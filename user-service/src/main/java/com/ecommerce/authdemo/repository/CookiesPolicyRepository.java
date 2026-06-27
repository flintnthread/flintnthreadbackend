package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.CookiesPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CookiesPolicyRepository extends JpaRepository<CookiesPolicy, Integer> {
    Optional<CookiesPolicy> findTopByOrderByIdDesc();
}
