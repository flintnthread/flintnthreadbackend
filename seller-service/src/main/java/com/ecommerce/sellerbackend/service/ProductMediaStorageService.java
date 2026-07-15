package com.ecommerce.sellerbackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProductMediaStorageService {

    private static final Pattern DATA_URL =
            Pattern.compile("^data:image/([a-zA-Z0-9+.-]+);base64,(.+)$", Pattern.DOTALL);

    private final Path uploadRoot;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public ProductMediaStorageService(
            @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Persists a size chart image and returns a DB-safe relative path.
     */
    public String storeSizeChartImage(String source, Long sellerId) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Image source is required.");
        }
        String trimmed = source.trim();
        if (trimmed.startsWith("uploads/size_charts/") || trimmed.startsWith("uploads/products/")) {
            return trimmed;
        }
        if (trimmed.startsWith("data:")) {
            return saveBase64DataUrl(trimmed, "size_charts", sellerId);
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            if (trimmed.length() <= 255 && trimmed.contains("/uploads/")) {
                int idx = trimmed.indexOf("/uploads/");
                return trimmed.substring(idx + 1);
            }
            return downloadToUploads(trimmed, "size_charts", sellerId);
        }
        throw new IllegalArgumentException("Unsupported image source. Use a picked image or https URL.");
    }

    /**
     * Persists an image and returns a DB-safe relative path (e.g. uploads/products/abc.png).
     * Absolute CDN URLs that already point at /uploads/... are stored as relative paths
     * so admin / user / seller all resolve via the same public media base URL.
     */
    public String storeProductImage(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Image source is required.");
        }
        String trimmed = source.trim();
        if (trimmed.startsWith("uploads/")) {
            return trimmed;
        }
        if (trimmed.startsWith("/uploads/")) {
            return trimmed.substring(1);
        }
        if (trimmed.startsWith("data:")) {
            return saveBase64DataUrl(trimmed, "products", null);
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            String relative = extractUploadsRelativePath(trimmed);
            if (relative != null) {
                return relative;
            }
            if (trimmed.length() <= 255) {
                // External short URL — keep as-is (already public)
                return trimmed;
            }
            return downloadToUploads(trimmed, "products", null);
        }
        throw new IllegalArgumentException("Unsupported image source. Use a picked image or https URL.");
    }

    /** Prefer relative DB path: uploads/products/x.jpg from any host. */
    private static String extractUploadsRelativePath(String url) {
        int idx = url.indexOf("/uploads/");
        if (idx < 0) {
            return null;
        }
        String path = url.substring(idx + 1); // uploads/...
        if (path.length() > 255) {
            return null;
        }
        return path;
    }

    private String saveBase64DataUrl(String dataUrl, String folder, Long sellerId) {
        Matcher matcher = DATA_URL.matcher(dataUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid image data URL.");
        }
        String ext = extensionForMime(matcher.group(1));
        byte[] bytes = Base64.getDecoder().decode(matcher.group(2));
        return writeBytes(bytes, ext, folder, sellerId);
    }

    private String downloadToUploads(String url, String folder, Long sellerId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException("Failed to download image (" + response.statusCode() + ").");
            }
            String ext = guessExtensionFromUrl(url);
            return writeBytes(response.body(), ext, folder, sellerId);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Image download interrupted.");
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to download image: " + ex.getMessage());
        }
    }

    private String writeBytes(byte[] bytes, String ext, String folder, Long sellerId) {
        try {
            Path targetDir = uploadRoot.resolve(folder);
            Files.createDirectories(targetDir);
            String fileName = folder.equals("size_charts") && sellerId != null
                    ? "size_chart_" + sellerId + "_" + System.currentTimeMillis() / 1000 + ext
                    : UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = targetDir.resolve(fileName);
            Files.write(target, bytes);
            return "uploads/" + folder + "/" + fileName;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to save image: " + ex.getMessage());
        }
    }

    private static String extensionForMime(String mime) {
        String m = mime.toLowerCase(Locale.ROOT);
        if (m.contains("png")) return ".png";
        if (m.contains("webp")) return ".webp";
        if (m.contains("gif")) return ".gif";
        return ".jpg";
    }

    private static String guessExtensionFromUrl(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains(".png")) return ".png";
        if (lower.contains(".webp")) return ".webp";
        if (lower.contains(".gif")) return ".gif";
        return ".jpg";
    }
}
