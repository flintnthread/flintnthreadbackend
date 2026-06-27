package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ReferralOrderDiscountRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

    public interface ReferralDiscountRepository
            extends JpaRepository<ReferralOrderDiscountRedemption,Integer> {
    }

