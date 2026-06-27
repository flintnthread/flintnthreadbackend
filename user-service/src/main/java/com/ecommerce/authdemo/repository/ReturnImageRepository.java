package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ReturnImage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnImageRepository
        extends JpaRepository<ReturnImage, Long> {

    List<ReturnImage>
    findByReturnIdOrderByCreatedAtDesc(
            Long returnId
    );
}