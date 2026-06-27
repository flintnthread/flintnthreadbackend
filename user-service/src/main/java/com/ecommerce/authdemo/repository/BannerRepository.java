package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findByStatus(Integer status);
}
