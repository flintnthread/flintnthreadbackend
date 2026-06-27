package com.ecommerce.authdemo.repository;

import com.ecommerce.authdemo.entity.ExchangeImage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExchangeImageRepository
        extends JpaRepository<ExchangeImage, Long> {

    List<ExchangeImage>
    findByExchangeIdOrderByCreatedAtDesc(
            Long exchangeId
    );
}