package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.SizeChart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SizeChartRepository extends JpaRepository<SizeChart, Integer> {

    List<SizeChart> findBySellerIdAndIsActiveTrueOrderByUpdatedAtDesc(Long sellerId);

    Optional<SizeChart> findByIdAndSellerId(Integer id, Long sellerId);
}
