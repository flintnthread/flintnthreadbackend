package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.ExchangeImageRequest;
import com.ecommerce.authdemo.dto.ExchangeImageResponse;
import com.ecommerce.authdemo.service.ExchangeImageService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/exchange-images")
@RequiredArgsConstructor
public class ExchangeImageController {

    private final ExchangeImageService
            exchangeImageService;

    // =====================================
    // CREATE SINGLE IMAGE ENTRY
    // =====================================

    @PostMapping
    public ExchangeImageResponse create(
            @RequestBody
            ExchangeImageRequest request
    ) {

        return exchangeImageService.create(
                request
        );
    }


    @GetMapping("/{exchangeId}")
    public List<ExchangeImageResponse>
    getByExchangeId(

            @PathVariable Long exchangeId
    ) {

        return exchangeImageService
                .getByExchangeId(exchangeId);
    }


    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id
    ) {

        exchangeImageService.delete(id);
    }
    @PostMapping(
            value = "/upload/{exchangeId}",
            consumes = "multipart/form-data"
    )
    public void uploadExchangeImages(

            @PathVariable Long exchangeId,

            @RequestParam("files")
            List<MultipartFile> files
    ) {

        exchangeImageService.uploadExchangeImages(
                exchangeId,
                files
        );
    }

    @GetMapping("/gallery/{exchangeId}")
    public List<ExchangeImageResponse>
    getExchangeImages(

            @PathVariable Long exchangeId
    ) {

        return exchangeImageService
                .getExchangeImages(exchangeId);
    }
}