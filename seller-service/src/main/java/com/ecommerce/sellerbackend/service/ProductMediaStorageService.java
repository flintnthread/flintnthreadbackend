package com.ecommerce.sellerbackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Product images are stored on local disk under {@code uploads/products/}.
 * DB {@code product_images.image_path} holds the relative path only
 * (e.g. {@code uploads/products/{uuid}.jpg}). Public absolute URLs are built
 * by API layers via {@code app.media.public-base-url}.
 */
@Service
public class ProductMediaStorageService {

    private static final Pattern DATA_URL =
            Pattern.compile("^data:image/([a-zA-Z0-9+.-]+);base64,(.+)$", Pattern.DOTALL);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final Path productsRoot;
    private final Path sizeChartsRoot;
    private final String publicBaseUrl;

    public ProductMediaStorageService(
            @Value("${app.upload.dir:uploads}") String uploadDir,
            @Value("${app.upload.products-directory:}") String productsDirectoryOverride,
            @Value("${app.media.public-base-url:}") String publicBaseUrl
    ) {
        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (productsDirectoryOverride != null && !productsDirectoryOverride.isBlank()) {
            this.productsRoot = Paths.get(productsDirectoryOverride).toAbsolutePath().normalize();
        } else {
            this.productsRoot = uploadRoot.resolve("products");
        }
        this.sizeChartsRoot = uploadRoot.resolve("size_charts");
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/+$", "");
        try {
            Files.createDirectories(this.productsRoot);
            Files.createDirectories(this.sizeChartsRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create product upload directories", e);
        }
    }

    /**
     * Persist a product image and return the relative path for DB storage
     * ({@code uploads/products/...}).
     */
    public String storeProductImage(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Image source is required.");
        }
        String trimmed = source.trim();

        if (trimmed.startsWith("uploads/") || trimmed.startsWith("/uploads/")) {
            return normalizeUploadsRelative(trimmed);
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            String relative = extractUploadsRelativePath(trimmed);
            if (relative != null) {
                return relative;
            }
            throw new IllegalArgumentException(
                    "External image URLs are not supported. Upload the image file first.");
        }
        if (trimmed.startsWith("data:")) {
            return saveBytes(decodeDataUrl(trimmed), "jpg", productsRoot, "products");
        }
        throw new IllegalArgumentException("Unsupported image source. Upload a file via product-media/upload.");
    }

    public String storeSizeChartImage(String source, Long sellerId) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Image source is required.");
        }
        String trimmed = source.trim();
        if (trimmed.startsWith("uploads/") || trimmed.startsWith("/uploads/")) {
            return normalizeUploadsRelative(trimmed);
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            String relative = extractUploadsRelativePath(trimmed);
            if (relative != null) {
                return relative;
            }
            throw new IllegalArgumentException(
                    "Size chart HTTPS URL must be a local uploads path. Re-upload the image.");
        }
        if (trimmed.startsWith("data:")) {
            return saveBytes(decodeDataUrl(trimmed), "jpg", sizeChartsRoot, "size_charts");
        }
        throw new IllegalArgumentException("Unsupported image source. Use a picked image.");
    }

    /** Multipart upload — writes to disk, returns relative {@code uploads/products/...} path. */
    public String uploadMultipart(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Image size must not exceed 10 MB.");
        }
        String extension = resolveExtension(file);
        try {
            return saveBytes(file.getBytes(), extension, productsRoot, "products");
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to upload product image: " + ex.getMessage());
        }
    }

    /** Absolute URL for clients (preview). Based on {@code app.media.public-base-url}. */
    public String toPublicUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return "";
        }
        String normalized = normalizeUploadsRelative(relativePath);
        String path = "/" + normalized;
        if (publicBaseUrl.isBlank()) {
            return path;
        }
        return publicBaseUrl + path;
    }

    private String saveBytes(byte[] bytes, String extension, Path directory, String folderName) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Image bytes are empty.");
        }
        String ext = extension == null || extension.isBlank() ? "jpg" : extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Only JPG, PNG or WEBP images are allowed.");
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = directory.resolve(fileName).normalize();
        if (!target.startsWith(directory)) {
            throw new IllegalArgumentException("Invalid upload path.");
        }
        try {
            Files.createDirectories(directory);
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to save image file: " + e.getMessage());
        }
        return "uploads/" + folderName + "/" + fileName;
    }

    private byte[] decodeDataUrl(String dataUrl) {
        Matcher matcher = DATA_URL.matcher(dataUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid image data URL.");
        }
        return Base64.getDecoder().decode(matcher.group(2));
    }

    private String resolveExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            String ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (ALLOWED_EXTENSIONS.contains(ext)) {
                return ext.equals("jpeg") ? "jpg" : ext;
            }
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.contains("png")) {
                return "png";
            }
            if (contentType.contains("webp")) {
                return "webp";
            }
        }
        return "jpg";
    }

    private static String normalizeUploadsRelative(String path) {
        String normalized = path.replace('\\', '/').trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        normalized = normalized.replaceFirst("(?i)^(ads|pads)/products/", "uploads/products/");
        if (!normalized.startsWith("uploads/")) {
            if (!normalized.contains("/")) {
                return "uploads/products/" + normalized;
            }
            return "uploads/" + normalized;
        }
        return normalized;
    }

    private static String extractUploadsRelativePath(String url) {
        int idx = url.indexOf("/uploads/");
        if (idx < 0) {
            return null;
        }
        String path = url.substring(idx + 1);
        return path.length() > 1024 ? null : path;
    }
}
