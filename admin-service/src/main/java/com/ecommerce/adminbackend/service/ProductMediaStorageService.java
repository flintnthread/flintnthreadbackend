package com.ecommerce.adminbackend.service;

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

    private final Path productsDir;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public ProductMediaStorageService(
            @Value("${app.upload.products-directory:uploads/products}") String productsDirectory) {
        this.productsDir = Paths.get(productsDirectory).toAbsolutePath().normalize();
    }

    /**
     * Persists an image and returns a DB-safe relative path (e.g. uploads/products/abc.png).
     */
    public String storeProductImage(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Image source is required.");
        }
        String trimmed = source.trim();
        if (trimmed.startsWith("uploads/")) {
            return trimmed;
        }
        if (trimmed.startsWith("data:")) {
            return saveBase64DataUrl(trimmed);
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            if (trimmed.length() <= 255 && trimmed.contains("/uploads/")) {
                int idx = trimmed.indexOf("/uploads/");
                return trimmed.substring(idx + 1);
            }
            if (trimmed.length() <= 255) {
                return trimmed;
            }
            return downloadToUploads(trimmed);
        }
        throw new IllegalArgumentException("Unsupported image source. Use a picked image or https URL.");
    }

    private String saveBase64DataUrl(String dataUrl) {
        Matcher matcher = DATA_URL.matcher(dataUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid image data URL.");
        }
        String ext = extensionForMime(matcher.group(1));
        byte[] bytes = Base64.getDecoder().decode(matcher.group(2));
        return writeBytes(bytes, ext);
    }

    private String downloadToUploads(String url) {
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
            return writeBytes(response.body(), guessExtensionFromUrl(url));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("Image download interrupted.");
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to download image: " + ex.getMessage());
        }
    }

    private String writeBytes(byte[] bytes, String ext) {
        try {
            Files.createDirectories(productsDir);
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = productsDir.resolve(fileName).normalize();
            if (!target.startsWith(productsDir)) {
                throw new IllegalArgumentException("Invalid upload path.");
            }
            Files.write(target, bytes);
            return "uploads/products/" + fileName;
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
