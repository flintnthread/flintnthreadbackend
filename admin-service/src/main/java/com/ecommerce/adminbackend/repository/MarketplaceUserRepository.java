package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.MarketplaceUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceUserRepository extends JpaRepository<MarketplaceUser, Integer> {
}
