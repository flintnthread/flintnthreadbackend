package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.DeliveryPincode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryPincodeRepository extends JpaRepository<DeliveryPincode, Integer> {

    boolean existsByPincodeAndStatus(String pincode, Integer status);
}