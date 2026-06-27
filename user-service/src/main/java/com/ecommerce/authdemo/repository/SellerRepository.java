package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {
    Optional<Seller> findByEmail(String email);
    Optional<Seller> findByMobileNumber(String mobile);
}
