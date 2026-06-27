package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.DeliveryWeightSlab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryWeightSlabRepository extends JpaRepository<DeliveryWeightSlab, Long> {

    List<DeliveryWeightSlab> findAllByOrderBySortOrderAscIdAsc();
}
