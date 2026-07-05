package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.SellerKycImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SellerKycImageRepository extends JpaRepository<SellerKycImage, Long> {

    @Query("""
            SELECT k FROM SellerKycImage k
            WHERE k.sellerId IN :sellerIds
              AND k.docType = 'live_selfie'
            ORDER BY k.capturedAt DESC
            """)
    List<SellerKycImage> findLiveSelfiesBySellerIds(@Param("sellerIds") Collection<Long> sellerIds);

    @Query("""
            SELECT k FROM SellerKycImage k
            WHERE k.sellerId IN :sellerIds
              AND k.imageType = 'face'
              AND k.imagePath IS NOT NULL
              AND TRIM(k.imagePath) <> ''
            ORDER BY k.capturedAt DESC
            """)
    List<SellerKycImage> findFaceImagesBySellerIds(@Param("sellerIds") Collection<Long> sellerIds);

    List<SellerKycImage> findBySellerIdOrderByCapturedAtDesc(Long sellerId);

    long countBySellerId(Long sellerId);

    @Query("""
            SELECT k.sellerId, COUNT(k)
            FROM SellerKycImage k
            WHERE k.sellerId IN :sellerIds
            GROUP BY k.sellerId
            """)
    List<Object[]> countGroupedBySellerIds(@Param("sellerIds") Collection<Long> sellerIds);
}
