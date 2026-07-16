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
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class MediaStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final Path uploadRoot;
    private final String publicBaseUrl;

    public MediaStorageService(
            @Value("${app.upload.directory:uploads/seller_documents}") String uploadDirectory,
            @Value("${app.media.public-base-url:}") String publicBaseUrl) {
        this.uploadRoot = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/$", "");
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + this.uploadRoot, e);
        }
    }

    public StoredFile storeSellerDocument(Long sellerId, SellerDocumentType type, MultipartFile file)
            throws IOException {
        validateFile(file);

        String extension = resolveExtension(file);
        int sequence = type.isAllowMultiple() ? nextSequence(sellerId, type) : 0;
        String fileName = buildFileName(sellerId, type, sequence, extension);
        Path target = uploadRoot.resolve(fileName);

        try (var input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }

        String publicUrl = toAbsolutePublicUrl(fileName);
        if (publicUrl == null || publicUrl.isBlank()) {
            publicUrl = toPublicUrl(fileName);
        }
        return new StoredFile(fileName, publicUrl);
    }

    private int nextSequence(Long sellerId, SellerDocumentType type) {
        String prefix = sellerId + "_" + type.getFileToken() + "_";
        try (var stream = Files.list(uploadRoot)) {
            return (int) stream
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith(prefix))
                    .count() + 1;
        } catch (IOException e) {
            return 1;
        }
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

    private String buildFileName(Long sellerId, SellerDocumentType type, int sequence, String extension) {
        long timestamp = System.currentTimeMillis() / 1000L;
        if (type.isAllowMultiple()) {
            return sellerId + "_" + type.getFileToken() + "_" + sequence + "_" + timestamp + "." + extension;
        }
        return sellerId + "_" + type.getFileToken() + "_" + timestamp + "." + extension;
    }

    public String toPublicUrl(String fileName) {
        return SellerMediaUrlHelper.toPublicPath(fileName);
    }

    /** Full CDN URL when {@code app.media.public-base-url} is set (e.g. https://flintnthread.in). */
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
