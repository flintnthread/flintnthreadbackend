package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.MarketplaceUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketplaceUserRepository extends JpaRepository<MarketplaceUser, Integer> {

    Optional<MarketplaceUser> findFirstByEmailIgnoreCase(String email);
}
