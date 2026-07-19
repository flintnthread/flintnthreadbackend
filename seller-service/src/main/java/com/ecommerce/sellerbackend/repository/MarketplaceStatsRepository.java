package com.ecommerce.sellerbackend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class MarketplaceStatsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /** All registered marketplace users (not only those who placed an order). */
    public long countDistinctCustomers() {
        Number count = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM users
                """)
                .getSingleResult();
        return count.longValue();
    }

    public Double averageApprovalHours() {
        Object value = entityManager.createNativeQuery("""
                SELECT AVG(TIMESTAMPDIFF(HOUR, kyc_submitted_at, kyc_verified_at))
                FROM sellers
                WHERE kyc_submitted_at IS NOT NULL
                  AND kyc_verified_at IS NOT NULL
                  AND kyc_verified_at >= kyc_submitted_at
                  AND status = 'active'
                """)
                .getSingleResult();
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }
}
