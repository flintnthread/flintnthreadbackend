package com.ecommerce.sellerbackend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Product images are uploaded to Cloudinary. DB {@code product_images.image_path}
 * stores the absolute HTTPS {@code secure_url}. Admin and user services return that URL as-is.
 */
@Service
@RequiredArgsConstructor
public class ProductMediaStorageService {

    private static final Pattern DATA_URL =
            Pattern.compile("^data:image/([a-zA-Z0-9+.-]+);base64,(.+)$", Pattern.DOTALL);

    private final Cloudinary cloudinary;

    @Value("${app.cloudinary.folder-prefix:flintnthread}")
    private String cloudinaryFolderPrefix;

    /**
     * Persist a product image and return the absolute Cloudinary HTTPS URL
     * (or keep an already-public Cloudinary / legacy absolute URL / uploads path).
     */
    public String storeProductImage(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Image source is required.");
        }
        String trimmed = source.trim();

        if (isCloudinaryUrl(trimmed)) {
            return trimmed;
        }

        if ((trimmed.startsWith("http://") || trimmed.startsWith("https://"))
                && !trimmed.contains("/uploads/")) {
            return trimmed;
        }

        if (trimmed.startsWith("uploads/") || trimmed.startsWith("/uploads/")) {
            return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            String relative = extractUploadsRelativePath(trimmed);
            if (relative != null) {
                return relative;
            }
        }

        if (trimmed.startsWith("data:")) {
            return uploadBase64DataUrl(trimmed);
        }

        throw new IllegalArgumentException("Unsupported image source. Use a picked image or https URL.");
    }

    public String storeSizeChartImage(String source, Long sellerId) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Image source is required.");
        }
        String trimmed = source.trim();
        if (isCloudinaryUrl(trimmed) || (trimmed.startsWith("https://") && !trimmed.contains("/uploads/"))) {
            return trimmed;
        }
        if (trimmed.startsWith("uploads/") || trimmed.startsWith("/uploads/")) {
            return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        }
        if (trimmed.startsWith("data:")) {
            return uploadBytes(decodeDataUrl(trimmed), buildFolder("size_charts"));
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            String relative = extractUploadsRelativePath(trimmed);
            if (relative != null) {
                return relative;
            }
            throw new IllegalArgumentException(
                    "Size chart HTTPS URL must already be a Cloudinary link. Re-upload the image.");
        }
        throw new IllegalArgumentException("Unsupported image source. Use a picked image or https URL.");
    }

    /** Multipart upload — preferred path from seller app. Returns Cloudinary secure_url. */
    public String uploadMultipart(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required.");
        }
        try {
            return uploadBytes(file.getBytes(), buildFolder("products"));
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to upload product image: " + ex.getMessage());
        }
    }

    /** Absolute URL for clients — Cloudinary URLs are already absolute. */
    public String toPublicUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return "";
        }
        return storedPath.trim();
    }

    private String uploadBase64DataUrl(String dataUrl) {
        return uploadBytes(decodeDataUrl(dataUrl), buildFolder("products"));
    }

    private byte[] decodeDataUrl(String dataUrl) {
        Matcher matcher = DATA_URL.matcher(dataUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid image data URL.");
        }
        return Base64.getDecoder().decode(matcher.group(2));
    }

    @SuppressWarnings("rawtypes")
    private String uploadBytes(byte[] bytes, String folder) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Image bytes are empty.");
        }
        try {
            Map options = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image",
                    "overwrite", false
            );
            Map result = cloudinary.uploader().upload(bytes, options);
            Object secureUrl = result.get("secure_url");
            if (secureUrl == null || String.valueOf(secureUrl).isBlank()) {
                throw new IllegalArgumentException("Cloudinary did not return a secure_url.");
            }
            return String.valueOf(secureUrl).trim();
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cloudinary upload failed: " + ex.getMessage());
        }
    }

    private String buildFolder(String suffix) {
        String prefix = cloudinaryFolderPrefix == null ? "flintnthread" : cloudinaryFolderPrefix.trim();
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        String s = suffix == null ? "products" : suffix.trim();
        if (s.startsWith("/")) {
            s = s.substring(1);
        }
        return prefix.isBlank() ? s : prefix + "/" + s;
    }

    private static boolean isCloudinaryUrl(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("res.cloudinary.com/") || lower.contains("cloudinary.com/");
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
