package com.ecommerce.sellerbackend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class SupportFileStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String store(MultipartFile file, HttpServletRequest request) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        String extension = null;
        if (originalName.endsWith(".png")) {
            extension = ".png";
        } else if (originalName.endsWith(".webp")) {
            extension = ".webp";
        } else if (originalName.endsWith(".gif")) {
            extension = ".gif";
        } else if (originalName.endsWith(".jpg") || originalName.endsWith(".jpeg")) {
            extension = ".jpg";
        }

        if (extension == null) {
            extension = switch (contentType) {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                case "image/gif" -> ".gif";
                case "image/jpeg", "image/jpg" -> ".jpg";
                default -> null;
            };
        }

        if (extension == null) {
            throw new IllegalArgumentException("Only image files (JPEG, PNG, WEBP, GIF) are allowed");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        Path supportDir = Paths.get(uploadDir, "support").toAbsolutePath().normalize();
        Files.createDirectories(supportDir);

        Path target = supportDir.resolve(filename);
        Files.copy(file.getInputStream(), target);

        String baseUrl = buildBaseUrl(request);
        return baseUrl + "/uploads/support/" + filename;
    }

    private String buildBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
            return scheme + "://" + host;
        }
        return scheme + "://" + host + ":" + port;
    }
}
