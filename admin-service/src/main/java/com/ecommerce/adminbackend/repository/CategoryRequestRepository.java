package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.CategoryRequest;
import com.ecommerce.adminbackend.entity.CategoryRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRequestRepository extends JpaRepository<CategoryRequest, Long> {

    Page<CategoryRequest> findByStatusOrderByCreatedAtDesc(CategoryRequestStatus status, Pageable pageable);

    Page<CategoryRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(CategoryRequestStatus status);
}
