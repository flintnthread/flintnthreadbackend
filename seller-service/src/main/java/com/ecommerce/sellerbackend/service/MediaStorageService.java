package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.profile.SellerDocumentType;
import com.ecommerce.sellerbackend.util.SellerMediaUrlHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

/**
 * Seller KYC / identity document storage.
 * New uploads go to Cloudinary (secure_url stored in DB). Legacy local files under
 * {@code uploads/seller_documents} remain readable via {@link #toPublicUrl(String)}.
 */
@Service
public class MediaStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final Path uploadRoot;
    private final String publicBaseUrl;
    private final ProductMediaStorageService productMediaStorageService;

    public MediaStorageService(
            @Value("${app.upload.directory:uploads/seller_documents}") String uploadDirectory,
            @Value("${app.media.public-base-url:https://flintnthread.com}") String publicBaseUrl,
            ProductMediaStorageService productMediaStorageService) {
        this.uploadRoot = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/$", "");
        this.productMediaStorageService = productMediaStorageService;
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + this.uploadRoot, e);
        }
    }

    public StoredFile storeSellerDocument(Long sellerId, SellerDocumentType type, MultipartFile file) {
        validateFile(file);
        String cloudUrl = productMediaStorageService.uploadSellerDocument(file, type.getFileToken());
        if (cloudUrl == null || cloudUrl.isBlank()) {
            throw new IllegalStateException("Cloudinary did not return a secure_url for seller document.");
        }
        String trimmed = cloudUrl.trim();
        // DB stores the Cloudinary absolute URL (same pattern as profile_pic / products).
        return new StoredFile(trimmed, trimmed);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("File size must not exceed 10 MB.");
        }
        String extension = resolveExtension(file);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Only JPG, PNG, WEBP or PDF files are allowed.");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            String ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (ALLOWED_EXTENSIONS.contains(ext)) {
                return ext;
            }
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.contains("pdf")) {
                return "pdf";
            }
            if (contentType.contains("png")) {
                return "png";
            }
            if (contentType.contains("webp")) {
                return "webp";
            }
        }
        return "jpg";
    }

    /**
     * Relative public path for seller profile / KYC documents.
     * Cloudinary absolute URLs are returned unchanged.
     */
    public String toPublicUrl(String fileName) {
        return SellerMediaUrlHelper.toPublicPath(fileName);
    }

    /** Full public URL — Cloudinary URLs unchanged; legacy disk paths use CDN. */
    public String toAbsolutePublicUrl(String fileName) {
        return SellerMediaUrlHelper.toAbsoluteUrl(fileName, publicBaseUrl);
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }

    public void deleteSellerFiles(Long sellerId) {
        String prefix = sellerId + "_";
        try (var stream = Files.list(uploadRoot)) {
            stream
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // best effort cleanup
                        }
                    });
        } catch (IOException ignored) {
            // best effort cleanup
        }
    }

    public record StoredFile(String fileName, String publicUrl) {
    }
}
