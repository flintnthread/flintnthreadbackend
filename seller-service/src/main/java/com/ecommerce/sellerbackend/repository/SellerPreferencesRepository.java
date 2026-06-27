package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.SellerPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerPreferencesRepository extends JpaRepository<SellerPreferences, Long> {
}
