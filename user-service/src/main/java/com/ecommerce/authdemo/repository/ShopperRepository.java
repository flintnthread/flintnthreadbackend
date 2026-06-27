package com.ecommerce.authdemo.repository;


import com.ecommerce.authdemo.entity.Shopper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
    public interface ShopperRepository extends JpaRepository<Shopper, Integer> {

        List<Shopper> findByUserId(Long userId);

        Optional<Shopper> findByUserIdAndIsActiveTrue(Long userId);

        Optional<Shopper> findByIdAndUser_Id(Integer id, Long userId);
    }

