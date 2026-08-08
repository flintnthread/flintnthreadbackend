package com.ecommerce.adminbackend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Category / subcategory images → Cloudinary secure_url (same pattern as products).
 * CMS media (logos / banners) still store on local disk under uploads/cms.
 */
@Service
@RequiredArgsConstructor
public class CatalogImageStorageService {

    private final Cloudinary cloudinary;

    @Value("${app.upload.cms-directory:uploads/cms}")
    private String cmsDirectory;

    @Value("${app.cloudinary.folder-prefix:flintnthread}")
    private String cloudinaryFolderPrefix;

    /** Subcategory images → Cloudinary folder {@code flintnthread/subcategories}. */
    public String storeSubcategoryImage(MultipartFile file) {
        return uploadMultipartToCloudinary(file, "subcategories");
    }

    /** Category (main + nested) images → Cloudinary folder {@code flintnthread/categories}. */
    public String storeCategoryImage(MultipartFile file) {
        return uploadMultipartToCloudinary(file, "categories");
    }

    public String storeCmsMedia(MultipartFile file, String subfolder) {
        String folder = (subfolder == null || subfolder.isBlank()) ? "general" : subfolder.trim();
        return storeMultipartLocal(file, cmsDirectory + "/" + folder, "uploads/cms/" + folder);
    }

    /**
     * Persist a data-URL / http URL / relative path for subcategory images.
     * Data URLs are uploaded to Cloudinary; existing absolute / relative values are kept.
     */
    public String normalizeSubcategoryImageValue(String raw) {
        return normalizeImageValue(raw, "subcategories");
    }

    public String normalizeCategoryImageValue(String raw) {
        return normalizeImageValue(raw, "categories");
    }

    private String normalizeImageValue(String raw, String cloudinaryFolderSuffix) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (isCloudinaryUrl(value)) {
            return value;
        }
        if (value.regionMatches(true, 0, "data:image/", 0, "data:image/".length())) {
            return uploadBytesToCloudinary(decodeDataUrl(value), cloudinaryFolderSuffix);
        }
        return value;
    }

    private String uploadMultipartToCloudinary(MultipartFile file, String folderSuffix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        try {
            return uploadBytesToCloudinary(file.getBytes(), folderSuffix);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to store image: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("rawtypes")
    private String uploadBytesToCloudinary(byte[] bytes, String folderSuffix) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Image bytes are empty");
        }
        try {
            Map options = ObjectUtils.asMap(
                    "folder", buildFolder(folderSuffix),
                    "resource_type", "image",
                    "overwrite", false
            );
            Map result = cloudinary.uploader().upload(bytes, options);
            Object secureUrl = result.get("secure_url");
            if (secureUrl == null || String.valueOf(secureUrl).isBlank()) {
                throw new IllegalStateException("Cloudinary did not return a secure_url");
            }
            return String.valueOf(secureUrl).trim();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Cloudinary upload failed: " + ex.getMessage(), ex);
        }
    }

    private String buildFolder(String suffix) {
        String prefix = cloudinaryFolderPrefix == null ? "flintnthread" : cloudinaryFolderPrefix.trim();
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        String cleanSuffix = suffix == null ? "" : suffix.trim();
        if (cleanSuffix.startsWith("/")) {
            cleanSuffix = cleanSuffix.substring(1);
        }
        if (prefix.isBlank()) {
            return cleanSuffix;
        }
        if (cleanSuffix.isBlank()) {
            return prefix;
        }
        return prefix + "/" + cleanSuffix;
    }

    private static boolean isCloudinaryUrl(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("res.cloudinary.com/") || lower.contains("cloudinary.com/");
    }

    private static byte[] decodeDataUrl(String dataUrl) {
        int comma = dataUrl.indexOf(',');
        if (comma < 0) {
            throw new IllegalArgumentException("Invalid image data URL");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(comma + 1));
            if (bytes.length == 0) {
                throw new IllegalArgumentException("Empty image data");
            }
            return bytes;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decode image data URL: " + ex.getMessage(), ex);
        }
    }

    private String storeMultipartLocal(MultipartFile file, String directory, String publicPrefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        try {
            String extension = extensionFromContentType(file.getContentType(), file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
            Path dir = Paths.get(directory).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName).normalize();
            if (!target.startsWith(dir)) {
                throw new IllegalArgumentException("Invalid upload path");
            }
            file.transferTo(target);
            return publicPrefix + "/" + fileName;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store image: " + ex.getMessage(), ex);
        }
    }

    private String extensionFromContentType(String contentType, String originalFilename) {
        if (contentType != null) {
            String lower = contentType.toLowerCase(Locale.ROOT);
            if (lower.contains("png")) {
                return ".png";
            }
            if (lower.contains("webp")) {
                return ".webp";
            }
            if (lower.contains("gif")) {
                return ".gif";
            }
        }
        if (originalFilename != null) {
            String name = originalFilename.toLowerCase(Locale.ROOT);
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot < name.length() - 1) {
                return name.substring(dot);
            }
        }
        return ".jpg";
    }
}
