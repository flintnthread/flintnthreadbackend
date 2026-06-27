package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.AdminJobOpening;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminJobOpeningRepository extends JpaRepository<AdminJobOpening, Long> {

    List<AdminJobOpening> findAllByOrderByCreatedAtDesc();

    long countByDepartmentId(Long departmentId);
}
