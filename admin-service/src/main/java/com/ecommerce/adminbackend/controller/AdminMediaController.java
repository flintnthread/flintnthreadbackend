package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.util.MediaUrlHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves upload files locally when present; otherwise redirects to {@code app.media.public-base-url}.
 */
@RestController
@RequiredArgsConstructor
public class AdminMediaController {

    private final MediaUrlHelper mediaUrlHelper;

    @Value("${app.upload.sellers-directory:../seller-service/uploads/sellers}")
    private String sellersUploadDirectory;

    @Value("${app.upload.seller-documents-directory:../seller-service/uploads/seller_documents}")
    private String sellerDocumentsUploadDirectory;

    @Value("${app.upload.products-directory:../seller-service/uploads/products}")
    private String productsUploadDirectory;

    @Value("${app.upload.kyc-directory:../seller-service/uploads/kyc_images}")
    private String kycUploadDirectory;

    @Value("${app.upload.categories-directory:uploads/categories}")
    private String categoriesUploadDirectory;

    @Value("${app.upload.subcategories-directory:uploads/subcategories}")
    private String subcategoriesUploadDirectory;

    @Value("${app.upload.cms-directory:uploads/cms}")
    private String cmsUploadDirectory;

    @GetMapping("/uploads/categories/{filename:.+}")
    public ResponseEntity<?> categoryMedia(@PathVariable String filename) {
        return serveOrRedirect(
                categoriesUploadDirectory,
                "uploads/categories/" + filename,
                filename);
    }

    @GetMapping("/uploads/subcategories/{filename:.+}")
    public ResponseEntity<?> subcategoryMedia(@PathVariable String filename) {
        return serveOrRedirect(
                subcategoriesUploadDirectory,
                "uploads/subcategories/" + filename,
                filename);
    }

    @GetMapping("/uploads/cms/{*relativePath}")
    public ResponseEntity<?> cmsMedia(@PathVariable String relativePath) {
        String normalized = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        return serveOrRedirect(
                cmsUploadDirectory,
                "uploads/cms/" + normalized,
                normalized);
    }

    @GetMapping("/uploads/sellers/{filename:.+}")
    public ResponseEntity<?> sellerMedia(@PathVariable String filename) {
        if (mediaUrlHelper.isSellerDocumentFileName(filename)) {
            return serveOrRedirect(
                    sellerDocumentsUploadDirectory,
                    "uploads/seller_documents/" + filename,
                    filename);
        }
        return serveOrRedirect(sellersUploadDirectory, "uploads/sellers/" + filename, filename);
    }

    @GetMapping("/uploads/seller_documents/{filename:.+}")
    public ResponseEntity<?> sellerDocumentMedia(@PathVariable String filename) {
        return serveOrRedirect(
                sellerDocumentsUploadDirectory,
                "uploads/seller_documents/" + filename,
                filename);
    }

    @GetMapping("/uploads/products/{filename:.+}")
    public ResponseEntity<?> productMedia(@PathVariable String filename) {
        return serveLocalFile(productsUploadDirectory, filename);
    }

    @GetMapping("/uploads/kyc_images/{*relativePath}")
    public ResponseEntity<?> kycMedia(@PathVariable String relativePath) {
        String normalized = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        return serveOrRedirect(
                kycUploadDirectory,
                "uploads/kyc_images/" + normalized,
                normalized.contains("/") ? normalized : normalized);
    }

    private ResponseEntity<?> serveLocalFile(String directory, String fileName) {
        Path file = Paths.get(directory).toAbsolutePath().normalize().resolve(fileName).normalize();
        Path root = Paths.get(directory).toAbsolutePath().normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(resolveMediaType(fileName))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(resource);
    }

    private ResponseEntity<?> serveOrRedirect(String directory, String publicPath, String fileName) {
        ResponseEntity<?> local = serveLocalFile(directory, fileName);
        if (local.getStatusCode().is2xxSuccessful()) {
            return local;
        }
        // Product images: local disk only — no CDN redirect.
        if (publicPath != null && publicPath.startsWith("uploads/products/")) {
            return ResponseEntity.notFound().build();
        }
        String cdnUrl = mediaUrlHelper.toPublicUrl(publicPath);
        if (cdnUrl == null || cdnUrl.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(cdnUrl)).build();
    }

    private MediaType resolveMediaType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (lower.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        if (lower.endsWith(".webm")) {
            return MediaType.parseMediaType("video/webm");
        }
        return MediaType.IMAGE_JPEG;
    }
}
