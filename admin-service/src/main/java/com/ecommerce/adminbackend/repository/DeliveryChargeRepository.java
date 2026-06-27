package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.DeliveryCharge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryChargeRepository extends JpaRepository<DeliveryCharge, Integer> {

    List<DeliveryCharge> findAllByOrderByWeightMinAscIdAsc();
}
