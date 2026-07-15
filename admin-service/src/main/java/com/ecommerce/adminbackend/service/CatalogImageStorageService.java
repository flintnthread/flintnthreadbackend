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
import java.util.UUID;

@Service
public class CatalogImageStorageService {

    @Value("${app.upload.subcategories-directory:uploads/subcategories}")
    private String subcategoriesDirectory;

    @Value("${app.upload.categories-directory:uploads/categories}")
    private String categoriesDirectory;

    @Value("${app.upload.cms-directory:uploads/cms}")
    private String cmsDirectory;

    public String storeSubcategoryImage(MultipartFile file) {
        return storeMultipart(file, subcategoriesDirectory, "uploads/subcategories");
    }

    public String storeCategoryImage(MultipartFile file) {
        return storeMultipart(file, categoriesDirectory, "uploads/categories");
    }

    public String storeCmsMedia(MultipartFile file, String subfolder) {
        String folder = (subfolder == null || subfolder.isBlank()) ? "general" : subfolder.trim();
        return storeMultipart(file, cmsDirectory + "/" + folder, "uploads/cms/" + folder);
    }

    /**
     * Persist a data-URL / http URL / relative path for subcategory images.
     * Returns a relative uploads path for newly decoded data URLs, otherwise the trimmed input.
     */
    public String normalizeSubcategoryImageValue(String raw) {
        return normalizeImageValue(raw, subcategoriesDirectory, "uploads/subcategories");
    }

    public String normalizeCategoryImageValue(String raw) {
        return normalizeImageValue(raw, categoriesDirectory, "uploads/categories");
    }

    private String normalizeImageValue(String raw, String directory, String publicPrefix) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.regionMatches(true, 0, "data:image/", 0, "data:image/".length())) {
            return storeDataUrl(value, directory, publicPrefix);
        }
        return value;
    }

    private String storeMultipart(MultipartFile file, String directory, String publicPrefix) {
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

    private String storeDataUrl(String dataUrl, String directory, String publicPrefix) {
        int comma = dataUrl.indexOf(',');
        if (comma < 0) {
            throw new IllegalArgumentException("Invalid image data URL");
        }
        String meta = dataUrl.substring(0, comma);
        String payload = dataUrl.substring(comma + 1);
        String extension = ".jpg";
        if (meta.toLowerCase(Locale.ROOT).contains("image/png")) {
            extension = ".png";
        } else if (meta.toLowerCase(Locale.ROOT).contains("image/webp")) {
            extension = ".webp";
        } else if (meta.toLowerCase(Locale.ROOT).contains("image/gif")) {
            extension = ".gif";
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(payload);
            if (bytes.length == 0) {
                throw new IllegalArgumentException("Empty image data");
            }
            String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
            Path dir = Paths.get(directory).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName).normalize();
            if (!target.startsWith(dir)) {
                throw new IllegalArgumentException("Invalid upload path");
            }
            Files.write(target, bytes);
            return publicPrefix + "/" + fileName;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decode image data URL: " + ex.getMessage(), ex);
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
