package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.cms.SiteLogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteLogoRepository extends JpaRepository<SiteLogo, Integer> {

    Optional<SiteLogo> findFirstByOrderByIdAsc();
}
