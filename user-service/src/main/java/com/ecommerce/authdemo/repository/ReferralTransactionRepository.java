package com.ecommerce.authdemo.repository;


import com.ecommerce.authdemo.entity.ReferralTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

    public interface ReferralTransactionRepository extends JpaRepository<ReferralTransaction,Long> {

        long countByReferrerIdAndStatus(Long referrerId,
                                        ReferralTransaction.Status status);

        boolean existsByReferredUserId(Long userId);

        boolean existsByReferredUserIdAndTransactionType(
                Long referredUserId,
                ReferralTransaction.TransactionType transactionType);
    }

