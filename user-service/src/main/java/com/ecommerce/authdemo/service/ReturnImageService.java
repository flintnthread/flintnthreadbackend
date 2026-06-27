package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.ReturnImageRequest;
import com.ecommerce.authdemo.dto.ReturnImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReturnImageService {

    ReturnImageResponse create(
            ReturnImageRequest request
    );

    List<ReturnImageResponse> getByReturnId(
            Long returnId
    );

    void delete(
            Long id
    );

    // =========================
    // MULTIPART IMAGE UPLOAD
    // =========================

    void uploadReturnImages(
            Long returnId,
            List<MultipartFile> files
    );

    List<ReturnImageResponse> getReturnImages(
            Long returnId
    );
}