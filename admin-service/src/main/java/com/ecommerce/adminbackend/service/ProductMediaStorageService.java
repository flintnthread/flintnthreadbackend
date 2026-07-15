package com.ecommerce.adminbackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Admin product images are stored on local disk (same shared folder as seller-service).
 * DB stores relative {@code uploads/products/...} paths only.
 */
@Service
public class ProductMediaStorageService {

    private static final Pattern DATA_URL =
            Pattern.compile("^data:image/([a-zA-Z0-9+.-]+);base64,(.+)$", Pattern.DOTALL);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final Path productsRoot;
    private final String publicBaseUrl;

    public ProductMediaStorageService(
            @Value("${app.upload.products-directory:../seller-service/uploads/products}") String productsDirectory,
            @Value("${app.media.public-base-url:}") String publicBaseUrl
    ) {
        this.productsRoot = Paths.get(productsDirectory).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/+$", "");
        try {
            Files.createDirectories(this.productsRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create products upload directory: " + this.productsRoot, e);
        }
    }

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
            return saveBytes(decodeDataUrl(trimmed), "jpg");
        }
        throw new IllegalArgumentException("Unsupported image source. Use a picked image.");
    }

    public String uploadMultipart(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Image size must not exceed 10 MB.");
        }
        try {
            return saveBytes(file.getBytes(), resolveExtension(file));
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to upload product image: " + ex.getMessage());
        }
    }

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

    private String saveBytes(byte[] bytes, String extension) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Image bytes are empty.");
        }
        String ext = extension == null || extension.isBlank() ? "jpg" : extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Only JPG, PNG or WEBP images are allowed.");
        }
        if ("jpeg".equals(ext)) {
            ext = "jpg";
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = productsRoot.resolve(fileName).normalize();
        if (!target.startsWith(productsRoot)) {
            throw new IllegalArgumentException("Invalid upload path.");
        }
        try {
            Files.createDirectories(productsRoot);
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to save image file: " + e.getMessage());
        }
        return "uploads/products/" + fileName;
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
                return ext;
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
