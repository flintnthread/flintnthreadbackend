package com.ecommerce.authdemo.repository;



import com.ecommerce.authdemo.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

    public interface UserWalletRepository extends JpaRepository<UserWallet, Integer> {

        Optional<UserWallet> findByUserId(Integer userId);
    }

