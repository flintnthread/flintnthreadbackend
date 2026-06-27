package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.ExchangeImageRequest;
import com.ecommerce.authdemo.dto.ExchangeImageResponse;
import com.ecommerce.authdemo.entity.ExchangeImage;
import com.ecommerce.authdemo.exception.OrderException;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.ExchangeImageRepository;
import com.ecommerce.authdemo.service.ExchangeImageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeImageServiceImpl
        implements ExchangeImageService {

    private final ExchangeImageRepository
            exchangeImageRepository;

    private static final String EXCHANGE_UPLOAD_DIR =
            "uploads/exchanges/";

    @Override
    @Transactional
    public ExchangeImageResponse create(
            ExchangeImageRequest request
    ) {

        if (request == null) {

            throw new OrderException(
                    "Invalid request"
            );
        }

        if (request.getImagePath() == null
                || request.getImagePath().trim().isBlank()) {

            throw new OrderException(
                    "Image path is required"
            );
        }

        ExchangeImage entity = ExchangeImage.builder()

                .exchangeId(
                        Long.valueOf(request.getExchangeId())
                )

                .imagePath(
                        request.getImagePath().trim()
                )

                .build();

        entity = exchangeImageRepository.save(entity);

        log.info(
                "Exchange image created for exchangeId={}",
                request.getExchangeId()
        );

        return toResponse(entity);
    }

    @Override
    public List<ExchangeImageResponse> getByExchangeId(
            Long exchangeId
    ) {

        return exchangeImageRepository
                .findByExchangeIdOrderByCreatedAtDesc(
                        exchangeId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(
            Long id
    ) {

        ExchangeImage entity =
                exchangeImageRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Exchange image not found"
                                ));

        try {

            if (entity.getImagePath() != null) {

                File file =
                        new File(entity.getImagePath());

                if (file.exists()) {
                    file.delete();
                }
            }

        } catch (Exception e) {

            log.error(
                    "Failed deleting exchange image file",
                    e
            );
        }

        exchangeImageRepository.delete(entity);

        log.info(
                "Exchange image deleted id={}",
                id
        );
    }

    // =====================================
    // MULTIPART IMAGE UPLOAD
    // =====================================

    @Override
    @Transactional
    public void uploadExchangeImages(
            Long exchangeId,
            List<MultipartFile> files
    ) {

        if (files == null || files.isEmpty()) {

            throw new OrderException(
                    "Images are required"
            );
        }

        try {

            File dir =
                    new File(EXCHANGE_UPLOAD_DIR);

            if (!dir.exists()) {
                dir.mkdirs();
            }

            for (MultipartFile file : files) {

                if (file.isEmpty()) {
                    continue;
                }

                String originalName =
                        file.getOriginalFilename();

                String extension = "";

                if (originalName != null
                        && originalName.contains(".")) {

                    extension =
                            originalName.substring(
                                    originalName.lastIndexOf(".")
                            );
                }

                String fileName =
                        UUID.randomUUID()
                                + extension;

                Path uploadPath =
                        Paths.get(
                                EXCHANGE_UPLOAD_DIR,
                                fileName
                        );

                Files.copy(
                        file.getInputStream(),
                        uploadPath,
                        StandardCopyOption.REPLACE_EXISTING
                );

                ExchangeImage image = ExchangeImage.builder()

                        .exchangeId(exchangeId)

                        .imagePath(
                                EXCHANGE_UPLOAD_DIR + fileName
                        )

                        .build();

                exchangeImageRepository.save(image);
            }

            log.info(
                    "Exchange images uploaded for exchangeId={}",
                    exchangeId
            );

        } catch (Exception e) {

            log.error(
                    "Failed uploading exchange images",
                    e
            );

            throw new RuntimeException(
                    "Failed to upload exchange images",
                    e
            );
        }
    }

    @Override
    public List<ExchangeImageResponse> getExchangeImages(
            Long exchangeId
    ) {

        return exchangeImageRepository
                .findByExchangeIdOrderByCreatedAtDesc(
                        exchangeId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ExchangeImageResponse toResponse(
            ExchangeImage entity
    ) {

        return ExchangeImageResponse.builder()

                .id(Math.toIntExact(entity.getId()))

                .exchangeId(Math.toIntExact(entity.getExchangeId()))

                .imagePath(entity.getImagePath())

                .createdAt(entity.getCreatedAt())

                .build();
    }
}