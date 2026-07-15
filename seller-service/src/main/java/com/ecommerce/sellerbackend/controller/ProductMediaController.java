package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.service.ProductMediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Multipart product-image upload to Cloudinary.
 * Returns absolute HTTPS secure_url for create/update payloads.
 */
@RestController
@RequestMapping("/api/seller/product-media")
@RequiredArgsConstructor
public class ProductMediaController {

    public static final String SELLER_ID_HEADER = "X-Seller-Id";

    private final ProductMediaStorageService productMediaStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadProductImage(
            @RequestHeader(SELLER_ID_HEADER) Long sellerId,
            @RequestParam("file") MultipartFile file) {
        if (sellerId == null || sellerId <= 0) {
            throw new IllegalArgumentException("X-Seller-Id header is required");
        }
        String url = productMediaStorageService.uploadMultipart(file);
        Map<String, String> body = new LinkedHashMap<>();
        body.put("url", url);
        body.put("imageUrl", url);
        body.put("imagePath", url);
        return body;
    }
}
