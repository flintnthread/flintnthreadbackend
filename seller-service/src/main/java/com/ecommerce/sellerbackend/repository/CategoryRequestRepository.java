package com.ecommerce.sellerbackend.repository;

import com.ecommerce.sellerbackend.entity.CategoryRequest;
import com.ecommerce.sellerbackend.entity.CategoryRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRequestRepository extends JpaRepository<CategoryRequest, Long> {

    List<CategoryRequest> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    Optional<CategoryRequest> findByIdAndSellerId(Long id, Long sellerId);

    boolean existsBySellerIdAndCategoryNameIgnoreCaseAndStatus(
            Long sellerId,
            String categoryName,
            CategoryRequestStatus status);
}
