package com.ecommerce.authdemo.repository;





import com.ecommerce.authdemo.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

    public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Integer> {

        List<WalletTransaction> findByUserId(Integer userId);

        List<WalletTransaction> findByOrderId(Integer orderId);

        List<WalletTransaction> findByUserIdAndOrderId(Integer userId, Integer orderId);

        boolean existsByUserIdAndDescription(Integer userId, String description);

        boolean existsByUserIdAndDescriptionStartingWith(Integer userId, String descriptionPrefix);

        java.util.Optional<WalletTransaction> findFirstByUserIdAndDescription(
                Integer userId,
                String description
        );
    }

