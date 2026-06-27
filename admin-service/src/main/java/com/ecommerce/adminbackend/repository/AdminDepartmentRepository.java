package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.AdminDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminDepartmentRepository extends JpaRepository<AdminDepartment, Long> {

    List<AdminDepartment> findAllByOrderByNameAsc();
}
