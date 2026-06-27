package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.Seller;
import com.ecommerce.sellerbackend.entity.SellerAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    Optional<Seller> findByEmailIgnoreCase(String email);

    Optional<Seller> findFirstByGstNumberIgnoreCase(String gstNumber);

    boolean existsByGstNumberIgnoreCaseAndIdNot(String gstNumber, Long id);

    Optional<Seller> findByPasswordResetToken(String passwordResetToken);

    Optional<Seller> findByEmailVerificationToken(String emailVerificationToken);

    boolean existsByEmailIgnoreCase(String email);

    @Query(
            value = """
                    SELECT COUNT(*) FROM sellers
                    WHERE REPLACE(REPLACE(REPLACE(mobile, ' ', ''), '+', ''), '-', '') LIKE CONCAT('%', :digits)
                    """,
            nativeQuery = true)
    long countByMobileDigits(@Param("digits") String digits);

    @Query(
            value = """
                    SELECT * FROM sellers
                    WHERE REPLACE(REPLACE(REPLACE(mobile, ' ', ''), '+', ''), '-', '') LIKE CONCAT('%', :digits)
                    LIMIT 1
                    """,
            nativeQuery = true)
    Optional<Seller> findByMobileDigits(@Param("digits") String digits);

    long countByReferredBySellerId(Long referredBySellerId);

    @Query(
            value = """
                    SELECT COUNT(*) FROM sellers s
                    WHERE s.referred_by_seller_id = :sellerId
                      AND s.profile_completed = 1
                      AND EXISTS (
                        SELECT 1 FROM products p WHERE p.seller_id = s.id LIMIT 1
                      )
                    """,
            nativeQuery = true)
    long countQualifiedReferrals(@Param("sellerId") Long sellerId);

    boolean existsByReferralCodeIgnoreCase(String referralCode);

    boolean existsBySellerUniqueIdIgnoreCase(String sellerUniqueId);

    long countByStatus(SellerAccountStatus status);
}
