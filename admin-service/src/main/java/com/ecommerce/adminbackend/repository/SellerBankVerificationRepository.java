package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.SellerBankVerification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SellerBankVerificationRepository extends JpaRepository<SellerBankVerification, Long> {

    @Modifying
    @Query(value = """
            UPDATE seller_bank_verifications
            SET status = 'expired', updated_at = CURRENT_TIMESTAMP
            WHERE status IN ('pending', 'processing')
              AND expires_at IS NOT NULL
              AND expires_at < NOW()
            """, nativeQuery = true)
    int expireOverdue();

    @Query(value = """
            SELECT effective_status, COUNT(*) AS cnt
            FROM (
                SELECT CASE
                           WHEN v.status IN ('pending', 'processing')
                                AND v.expires_at IS NOT NULL
                                AND v.expires_at < NOW() THEN 'expired'
                           ELSE LOWER(v.status)
                       END AS effective_status
                FROM seller_bank_verifications v
                INNER JOIN (
                    SELECT seller_id, MAX(id) AS max_id
                    FROM seller_bank_verifications
                    GROUP BY seller_id
                ) latest ON latest.max_id = v.id
            ) t
            GROUP BY effective_status
            """, nativeQuery = true)
    List<Object[]> countLatestByEffectiveStatus();

    @Query(value = """
            SELECT DISTINCT seller_id FROM seller_bank_verifications
            """, nativeQuery = true)
    List<Long> findDistinctSellerIds();

    @Query(value = """
            SELECT v.*
            FROM seller_bank_verifications v
            INNER JOIN (
                SELECT seller_id, MAX(id) AS max_id
                FROM seller_bank_verifications
                GROUP BY seller_id
            ) latest ON latest.max_id = v.id
            WHERE (
                CASE
                    WHEN v.status IN ('pending', 'processing')
                         AND v.expires_at IS NOT NULL
                         AND v.expires_at < NOW() THEN 'expired'
                    ELSE LOWER(v.status)
                END
            ) = LOWER(:status)
            ORDER BY v.updated_at DESC, v.id DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM seller_bank_verifications v
            INNER JOIN (
                SELECT seller_id, MAX(id) AS max_id
                FROM seller_bank_verifications
                GROUP BY seller_id
            ) latest ON latest.max_id = v.id
            WHERE (
                CASE
                    WHEN v.status IN ('pending', 'processing')
                         AND v.expires_at IS NOT NULL
                         AND v.expires_at < NOW() THEN 'expired'
                    ELSE LOWER(v.status)
                END
            ) = LOWER(:status)
            """,
            nativeQuery = true)
    Page<SellerBankVerification> findLatestByEffectiveStatus(@Param("status") String status, Pageable pageable);

    Optional<SellerBankVerification> findFirstBySellerIdOrderByIdDesc(Long sellerId);
}
