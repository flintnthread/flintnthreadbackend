package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.ProductPincode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductPincodeRepository extends JpaRepository<ProductPincode, Long> {

    List<ProductPincode> findByProductId(Long productId);

    void deleteByProductId(Long productId);
}
