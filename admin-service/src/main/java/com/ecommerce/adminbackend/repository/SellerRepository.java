package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.Seller;
import com.ecommerce.adminbackend.entity.SellerAccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    @Query("""
            SELECT s FROM Seller s
            WHERE s.profileCompleted = true
              AND s.profileNeedsVerification = true
            ORDER BY s.profileUpdatedAt DESC, s.updatedAt DESC
            """)
    List<Seller> findPendingProfileReviews();

    @Query("""
            SELECT s FROM Seller s
            WHERE (:status IS NULL OR s.status = :status)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(s.businessName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(s.sellerUniqueId, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(s.referralCode, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   s.mobile LIKE CONCAT('%', :search, '%'))
            """)
    Page<Seller> searchSellers(@Param("status") SellerAccountStatus status,
                               @Param("search") String search,
                               Pageable pageable);

    @Query("""
            SELECT s FROM Seller s
            WHERE (:search IS NULL OR :search = '' OR
                   LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(s.businessName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(s.sellerUniqueId, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(s.referralCode, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   s.mobile LIKE CONCAT('%', :search, '%'))
            ORDER BY s.updatedAt DESC, s.createdAt DESC
            """)
    Page<Seller> searchSellersAll(@Param("search") String search, Pageable pageable);

    @Query("""
            SELECT s FROM Seller s
            WHERE s.bankName IS NOT NULL
              AND (s.bankVerified IS NULL OR s.bankVerified = false)
            ORDER BY s.updatedAt DESC
            """)
    Page<Seller> findPendingBankVerification(Pageable pageable);

    long countByStatus(SellerAccountStatus status);

    @Query(value = """
            SELECT COUNT(*) FROM sellers
            WHERE status IS NULL OR TRIM(status) = ''
               OR status IN ('pending', 'email_pending')
            """, nativeQuery = true)
    long countPendingActivation();

    @Query(value = """
            SELECT COUNT(*) FROM sellers
            WHERE profile_completed = 1
              AND (warehouse_address IS NULL OR TRIM(warehouse_address) = '')
            """, nativeQuery = true)
    long countShiprocketPending();

    @Query(value = """
            SELECT COUNT(*) FROM sellers
            WHERE warehouse_address IS NOT NULL
              AND TRIM(warehouse_address) <> ''
            """, nativeQuery = true)
    long countShiprocketUploaded();

    @Query("""
            SELECT COUNT(s) FROM Seller s
            WHERE s.bankName IS NOT NULL
              AND (s.bankVerified IS NULL OR s.bankVerified = false)
            """)
    long countPendingBankVerification();

    @Query("""
            SELECT COUNT(s) FROM Seller s
            WHERE s.bankVerified = true
            """)
    long countBankVerified();

    @Query(value = """
            SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) AS cnt
            FROM sellers
            WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH)
            GROUP BY DATE_FORMAT(created_at, '%Y-%m')
            ORDER BY month
            """, nativeQuery = true)
    List<Object[]> sellerRegistrationChart();

    @Query("""
            SELECT COUNT(s) FROM Seller s
            WHERE s.profileCompleted = true
              AND (:sellerId IS NULL OR s.id = :sellerId)
              AND COALESCE(s.profileUpdatedAt, s.updatedAt, s.createdAt) <= :endDate
            """)
    long countProfileCompletedOnOrBefore(@Param("sellerId") Long sellerId, @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COUNT(s) FROM Seller s
            WHERE s.profileCompleted = true
              AND (s.profileNeedsVerification = false OR s.profileNeedsVerification IS NULL)
              AND s.status = com.ecommerce.adminbackend.entity.SellerAccountStatus.active
              AND (:sellerId IS NULL OR s.id = :sellerId)
              AND COALESCE(s.kycVerifiedAt, s.profileUpdatedAt, s.updatedAt) <= :endDate
            """)
    long countApprovedOnOrBefore(@Param("sellerId") Long sellerId, @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COUNT(s) FROM Seller s
            WHERE (:sellerId IS NULL OR s.id = :sellerId)
              AND s.createdAt >= :startDate
              AND s.createdAt <= :endDate
            """)
    long countRegisteredInPeriod(@Param("sellerId") Long sellerId,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COUNT(s) FROM Seller s
            WHERE s.profileCompleted = true
              AND (:sellerId IS NULL OR s.id = :sellerId)
              AND COALESCE(s.profileUpdatedAt, s.updatedAt, s.createdAt) >= :startDate
              AND COALESCE(s.profileUpdatedAt, s.updatedAt, s.createdAt) <= :endDate
            """)
    long countProfileCompletedInPeriod(@Param("sellerId") Long sellerId,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COUNT(s) FROM Seller s
            WHERE s.profileCompleted = true
              AND (s.profileNeedsVerification = false OR s.profileNeedsVerification IS NULL)
              AND s.status = com.ecommerce.adminbackend.entity.SellerAccountStatus.active
              AND (:sellerId IS NULL OR s.id = :sellerId)
              AND COALESCE(s.kycVerifiedAt, s.profileUpdatedAt, s.updatedAt) >= :startDate
              AND COALESCE(s.kycVerifiedAt, s.profileUpdatedAt, s.updatedAt) <= :endDate
            """)
    long countApprovedInPeriod(@Param("sellerId") Long sellerId,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COUNT(s) FROM Seller s
            WHERE s.warehouseAddress IS NOT NULL
              AND TRIM(s.warehouseAddress) <> ''
              AND (:sellerId IS NULL OR s.id = :sellerId)
              AND s.updatedAt >= :startDate
              AND s.updatedAt <= :endDate
            """)
    long countShiprocketReadyInPeriod(@Param("sellerId") Long sellerId,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COUNT(s) FROM Seller s
            WHERE (:sellerId IS NULL OR s.id = :sellerId)
              AND s.createdAt <= :endDate
            """)
    long countRegisteredOnOrBefore(@Param("sellerId") Long sellerId, @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COUNT(s) FROM Seller s
            WHERE s.warehouseAddress IS NOT NULL
              AND TRIM(s.warehouseAddress) <> ''
              AND (:sellerId IS NULL OR s.id = :sellerId)
              AND s.updatedAt <= :endDate
            """)
    long countShiprocketReadyOnOrBefore(@Param("sellerId") Long sellerId, @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT COUNT(s) FROM Seller s
            WHERE s.profileCompleted = true
              AND (:sellerId IS NULL OR s.id = :sellerId)
            """)
    long countProfileCompleted(@Param("sellerId") Long sellerId);

    @Query("""
            SELECT COUNT(s) FROM Seller s
            WHERE s.profileCompleted = true
              AND (s.profileNeedsVerification = false OR s.profileNeedsVerification IS NULL)
              AND s.status = com.ecommerce.adminbackend.entity.SellerAccountStatus.active
              AND (:sellerId IS NULL OR s.id = :sellerId)
            """)
    long countApproved(@Param("sellerId") Long sellerId);

    @Query("""
            SELECT COUNT(s) FROM Seller s
            WHERE s.warehouseAddress IS NOT NULL
              AND TRIM(s.warehouseAddress) <> ''
              AND (:sellerId IS NULL OR s.id = :sellerId)
            """)
    long countShiprocketReady(@Param("sellerId") Long sellerId);

    @Query("""
            SELECT MIN(YEAR(s.createdAt)) FROM Seller s
            """)
    Integer findEarliestRegistrationYear();

    @Query("""
            SELECT s FROM Seller s
            WHERE (:search IS NULL OR :search = '' OR
                   LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(s.businessName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(s.sellerUniqueId, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(s.referralCode, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   s.mobile LIKE CONCAT('%', :search, '%'))
              AND (:sellerId IS NULL OR s.id = :sellerId)
            ORDER BY s.createdAt DESC
            """)
    Page<Seller> searchSellersForGraph(@Param("search") String search,
                                       @Param("sellerId") Long sellerId,
                                       Pageable pageable);

    @Query("""
            SELECT s FROM Seller s
            WHERE s.profileCompleted = true
              AND (s.warehouseAddress IS NULL OR LENGTH(TRIM(s.warehouseAddress)) = 0)
            ORDER BY s.profileUpdatedAt DESC, s.updatedAt DESC
            """)
    Page<Seller> findShiprocketPending(Pageable pageable);

    @Query("""
            SELECT s FROM Seller s
            WHERE s.warehouseAddress IS NOT NULL
              AND LENGTH(TRIM(s.warehouseAddress)) > 0
            ORDER BY s.updatedAt DESC
            """)
    Page<Seller> findShiprocketUploaded(Pageable pageable);

    @Query("""
            SELECT s FROM Seller s
            WHERE s.bankName IS NOT NULL
              AND s.bankVerified = true
            ORDER BY s.updatedAt DESC
            """)
    Page<Seller> findBankVerified(Pageable pageable);

    @Query("""
            SELECT COUNT(s) FROM Seller s
            WHERE NOT EXISTS (SELECT 1 FROM Product p WHERE p.sellerId = s.id)
            """)
    long countWithoutProducts();

    @Query("""
            SELECT s FROM Seller s
            WHERE s.profileCompleted = true
              AND (s.profileNeedsVerification = false OR s.profileNeedsVerification IS NULL)
              AND s.status IN (com.ecommerce.adminbackend.entity.SellerAccountStatus.active,
                                 com.ecommerce.adminbackend.entity.SellerAccountStatus.suspended)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(s.businessName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   s.mobile LIKE CONCAT('%', :search, '%'))
            ORDER BY s.updatedAt DESC, s.createdAt DESC
            """)
    Page<Seller> findAdminApproved(@Param("search") String search, Pageable pageable);

    @Query(value = """
            SELECT COALESCE(NULLIF(TRIM(state), ''), NULLIF(TRIM(warehouse_state), '')) AS location,
                   COUNT(*) AS total
            FROM sellers
            WHERE profile_completed = 1
              AND (profile_needs_verification = 0 OR profile_needs_verification IS NULL)
              AND status IN ('active', 'suspended')
              AND COALESCE(NULLIF(TRIM(state), ''), NULLIF(TRIM(warehouse_state), '')) IS NOT NULL
            GROUP BY location
            ORDER BY total DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> countAdminApprovedByState();

    @Query(value = """
            SELECT COALESCE(NULLIF(TRIM(city), ''), NULLIF(TRIM(warehouse_city), '')) AS location,
                   COUNT(*) AS total
            FROM sellers
            WHERE profile_completed = 1
              AND (profile_needs_verification = 0 OR profile_needs_verification IS NULL)
              AND status IN ('active', 'suspended')
              AND COALESCE(NULLIF(TRIM(city), ''), NULLIF(TRIM(warehouse_city), '')) IS NOT NULL
            GROUP BY location
            ORDER BY total DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> countAdminApprovedByCity();

    @Query("""
            SELECT s FROM Seller s
            ORDER BY LOWER(COALESCE(NULLIF(TRIM(s.firstName), ''), s.businessName, s.email)) ASC,
                     LOWER(COALESCE(s.lastName, '')) ASC,
                     s.id ASC
            """)
    List<Seller> findAllOrderedByName();
}
