package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.AdminJobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminJobApplicationRepository extends JpaRepository<AdminJobApplication, Long> {

    Page<AdminJobApplication> findAllByOrderByAppliedAtDesc(Pageable pageable);

    Page<AdminJobApplication> findByJobIdOrderByAppliedAtDesc(Long jobId, Pageable pageable);

    long countByJobId(Long jobId);
}
