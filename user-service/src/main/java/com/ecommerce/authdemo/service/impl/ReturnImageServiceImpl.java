package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.ReturnImageRequest;
import com.ecommerce.authdemo.dto.ReturnImageResponse;
import com.ecommerce.authdemo.entity.ReturnImage;
import com.ecommerce.authdemo.exception.OrderException;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.ReturnImageRepository;
import com.ecommerce.authdemo.service.ReturnImageService;

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
public class ReturnImageServiceImpl
        implements ReturnImageService {

    private final ReturnImageRepository returnImageRepository;

    private static final String RETURN_UPLOAD_DIR =
            "uploads/returns/";

    @Override
    @Transactional
    public ReturnImageResponse create(
            ReturnImageRequest request
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

        ReturnImage entity = ReturnImage.builder()

                .returnId(Long.valueOf(request.getReturnId()))

                .imagePath(
                        request.getImagePath().trim()
                )

                .build();

        entity = returnImageRepository.save(entity);

        log.info(
                "Return image created for returnId={}",
                request.getReturnId()
        );

        return toResponse(entity);
    }

    @Override
    public List<ReturnImageResponse> getByReturnId(
            Long returnId
    ) {

        return returnImageRepository
                .findByReturnIdOrderByCreatedAtDesc(
                        returnId
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

        ReturnImage entity =
                returnImageRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Return image not found"
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
                    "Failed deleting return image file",
                    e
            );
        }

        returnImageRepository.delete(entity);

        log.info(
                "Return image deleted id={}",
                id
        );
    }

    // =====================================
    // MULTIPART IMAGE UPLOAD
    // =====================================

    @Override
    @Transactional
    public void uploadReturnImages(
            Long returnId,
            List<MultipartFile> files
    ) {

        if (files == null || files.isEmpty()) {

            throw new OrderException(
                    "Images are required"
            );
        }

        try {

            File dir =
                    new File(RETURN_UPLOAD_DIR);

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
                                RETURN_UPLOAD_DIR,
                                fileName
                        );

                Files.copy(
                        file.getInputStream(),
                        uploadPath,
                        StandardCopyOption.REPLACE_EXISTING
                );

                ReturnImage image = ReturnImage.builder()

                        .returnId(returnId)

                        .imagePath(
                                RETURN_UPLOAD_DIR + fileName
                        )

                        .build();

                returnImageRepository.save(image);
            }

            log.info(
                    "Return images uploaded for returnId={}",
                    returnId
            );

        } catch (Exception e) {

            log.error(
                    "Failed uploading return images",
                    e
            );

            throw new RuntimeException(
                    "Failed to upload return images",
                    e
            );
        }
    }

    @Override
    public List<ReturnImageResponse> getReturnImages(
            Long returnId
    ) {

        return returnImageRepository
                .findByReturnIdOrderByCreatedAtDesc(
                        returnId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ReturnImageResponse toResponse(
            ReturnImage entity
    ) {

        return ReturnImageResponse.builder()

                .id(entity.getId())

                .returnId(entity.getReturnId())

                .imagePath(entity.getImagePath())

                .createdAt(entity.getCreatedAt())

                .build();
    }
}