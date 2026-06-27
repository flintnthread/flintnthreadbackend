package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ProductPincode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

    public interface ProductPincodeRepository extends JpaRepository<ProductPincode, Long> {

        Optional<ProductPincode> findByProductIdAndPincodeIdAndStatus(
                Long productId,
                Long pincodeId,
                Integer status
        );
    }

