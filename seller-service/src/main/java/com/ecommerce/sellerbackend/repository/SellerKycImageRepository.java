package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.SellerKycImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SellerKycImageRepository extends JpaRepository<SellerKycImage, Long> {

    List<SellerKycImage> findBySellerIdAndDocTypeOrderByCapturedAtDesc(Long sellerId, String docType);

    void deleteBySellerId(Long sellerId);
}
