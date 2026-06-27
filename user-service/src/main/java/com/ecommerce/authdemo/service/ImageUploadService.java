package com.ecommerce.authdemo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final Cloudinary cloudinary;
    @Value("${app.upload.max-file-size-bytes:26214400}")
    private long maxFileSizeBytes;

    @Value("${app.cloudinary.folder-prefix:flintnthread}")
    private String cloudinaryFolderPrefix;

    public String uploadImage(MultipartFile file) {
        return uploadImage(file, null);
    }

    public String uploadImage(MultipartFile file, String folderSuffix) {
        validateFile(file);
        try {
            Map<String, Object> options = new HashMap<>();
            String folder = buildFolder(folderSuffix);
            if (!folder.isBlank()) {
                options.put("folder", folder);
            }

            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    options
            );

            return uploadResult.get("secure_url").toString();

        } catch (Exception e) {
            e.printStackTrace(); // 🔥 important
            throw new RuntimeException("Image upload failed: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException(
                    "Image too large. Max allowed: " + (maxFileSizeBytes / (1024 * 1024)) + "MB"
            );
        }
    }

    private String buildFolder(String folderSuffix) {
        String prefix = String.valueOf(cloudinaryFolderPrefix).trim();
        String suffix = String.valueOf(folderSuffix == null ? "" : folderSuffix).trim();
        if (prefix.endsWith("/")) prefix = prefix.substring(0, prefix.length() - 1);
        if (suffix.startsWith("/")) suffix = suffix.substring(1);
        if (prefix.isBlank()) return suffix;
        if (suffix.isBlank()) return prefix;
        return prefix + "/" + suffix;
    }
}