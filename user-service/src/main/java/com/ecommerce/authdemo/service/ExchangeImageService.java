package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.ExchangeImageRequest;
import com.ecommerce.authdemo.dto.ExchangeImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ExchangeImageService {

    ExchangeImageResponse create(
            ExchangeImageRequest request
    );

    List<ExchangeImageResponse> getByExchangeId(
            Long exchangeId
    );

    void delete(
            Long id
    );

    // =====================================
    // MULTIPART IMAGE UPLOAD
    // =====================================

    void uploadExchangeImages(
            Long exchangeId,
            List<MultipartFile> files
    );

    List<ExchangeImageResponse> getExchangeImages(
            Long exchangeId
    );
}