package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.profile.SellerDocumentType;
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
import java.util.regex.Pattern;

@Service
public class MediaStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    /**
     * KYC / profile files on CDN live under {@code /uploads/seller_documents/}, e.g.
     * {@code https://flintnthread.in/uploads/seller_documents/12_aadhar_front_1759064059.png}
     */
    private static final Pattern SELLER_DOCUMENT_FILE = Pattern.compile(
            "^\\d+_(profile_pic|aadhar_front|aadhar_back|pan_card|business_proof|bank_proof|"
                    + "cancelled_cheque|live_selfie|company_pan_doc|incorporation_certificate|"
                    + "partnership_deed|msme_certificate|iec_certificate)(_|\\.)",
            Pattern.CASE_INSENSITIVE);

    private final Path uploadRoot;
    private final String publicBaseUrl;

    public MediaStorageService(
            @Value("${app.upload.directory:uploads/sellers}") String uploadDirectory,
            @Value("${app.media.public-base-url:https://flintnthread.in}") String publicBaseUrl) {
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

        return new StoredFile(fileName, toPublicUrl(fileName));
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

    /**
     * Public URL for seller profile / KYC documents.
     * Production CDN path: {@code https://flintnthread.in/uploads/seller_documents/...}
     */
    public String toPublicUrl(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String trimmed = fileName.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.contains("res.cloudinary.com/") || lower.contains("cloudinary.com/")) {
                return trimmed;
            }
            int idx = trimmed.indexOf("/uploads/");
            if (idx >= 0) {
                String path = normalizePublicPath(trimmed.substring(idx));
                return publicBaseUrl.isBlank() ? path : publicBaseUrl + path;
            }
            return trimmed;
        }
        String path = normalizePublicPath(trimmed);
        if (publicBaseUrl.isBlank()) {
            return path;
        }
        return publicBaseUrl + path;
    }

    /** Normalize DB/filename values to {@code /uploads/seller_documents/...} when applicable. */
    public String normalizePublicPath(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replace('\\', '/').trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            int idx = normalized.indexOf("/uploads/");
            if (idx >= 0) {
                normalized = normalized.substring(idx);
            }
        }
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("uploads/sellers/")) {
            String base = normalized.substring("uploads/sellers/".length());
            if (isSellerDocumentFileName(base)) {
                return "/uploads/seller_documents/" + base;
            }
            return "/" + normalized;
        }
        if (normalized.startsWith("uploads/seller_documents/")) {
            return "/" + normalized;
        }
        if (normalized.startsWith("uploads/")) {
            return "/" + normalized;
        }
        String base = normalized.contains("/")
                ? normalized.substring(normalized.lastIndexOf('/') + 1)
                : normalized;
        if (isSellerDocumentFileName(base)) {
            return "/uploads/seller_documents/" + base;
        }
        if (!normalized.contains("/")) {
            return "/uploads/sellers/" + normalized;
        }
        return "/" + normalized;
    }

    private boolean isSellerDocumentFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        String base = fileName.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        return SELLER_DOCUMENT_FILE.matcher(base).find();
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
