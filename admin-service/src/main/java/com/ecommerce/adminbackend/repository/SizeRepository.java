package com.ecommerce.adminbackend.repository;

import com.ecommerce.adminbackend.entity.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SizeRepository extends JpaRepository<Size, Long> {

    List<Size> findAllByOrderBySizeNameAsc();
}
