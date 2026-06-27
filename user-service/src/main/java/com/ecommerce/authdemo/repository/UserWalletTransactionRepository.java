package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.UserWalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserWalletTransactionRepository extends JpaRepository<UserWalletTransaction, Integer> {

    List<UserWalletTransaction> findByUserIdOrderByCreatedAtDesc(Integer userId);

    boolean existsByUserIdAndOrderIdAndTypeAndDescriptionContaining(
            Integer userId,
            Integer orderId,
            UserWalletTransaction.Type type,
            String descriptionFragment
    );
}
