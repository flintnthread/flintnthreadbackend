package com.ecommerce.adminbackend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Resolves DB file paths to public CDN URLs using {@code app.media.public-base-url}.
 * Used for seller profile pics, bank proofs, KYC docs, product images, order item images, etc.
 */
@Component
public class MediaUrlHelper {

    private static final Pattern SELLER_DOCUMENT_FILE = Pattern.compile(
            "^\\d+_(profile_pic|aadhar_front|aadhar_back|pan_card|business_proof|bank_proof|"
                    + "cancelled_cheque|live_selfie|company_pan_doc|incorporation_certificate|"
                    + "partnership_deed|msme_certificate|iec_certificate)(_|\\.)",
            Pattern.CASE_INSENSITIVE);

    private final String publicBaseUrl;

    public MediaUrlHelper(
            @Value("${app.media.public-base-url:https://flintnthread.com}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/$", "");
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    /**
     * Build a public URL for any upload stored in the database.
     */
    public String toPublicUrl(String path) {
        return toPublicUrl(path, null);
    }

    /**
     * Build a public URL. Absolute Cloudinary (or any https) URLs are returned unchanged —
     * never rewrite them onto the CDN host.
     */
    public String toPublicUrl(String path, String folder) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String trimmed = path.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            String lower = trimmed.toLowerCase();
            // Cloudinary / other absolute CDN URLs — use exactly what is stored.
            if (lower.contains("res.cloudinary.com/") || lower.contains("cloudinary.com/")) {
                return trimmed;
            }
            int idx = trimmed.indexOf("/uploads/");
            if (idx >= 0) {
                // Re-normalize so legacy /uploads/sellers/12_aadhar_front_... maps to seller_documents.
                String normalized = normalizeMediaPath(trimmed.substring(idx), folder);
                if (publicBaseUrl.isBlank()) {
                    return normalized;
                }
                return publicBaseUrl + normalized;
            }
            return trimmed;
        }
        String normalized = normalizeMediaPath(trimmed, folder);
        if (publicBaseUrl.isBlank()) {
            return normalized;
        }
        return publicBaseUrl + normalized;
    }

    /** @deprecated Use {@link #toPublicUrl(String)} — all media uses public-base-url. */
    @Deprecated
    public String toSellerMediaUrl(String path) {
        return toPublicUrl(path);
    }

    /**
     * DB may store bare filenames (e.g. 12_aadhar_front_123.jpg), relative uploads paths
     * (uploads/seller_documents/..., uploads/products/..., uploads/kyc_images/...), or /uploads/... paths.
     */
    public String normalizeMediaPath(String path) {
        return normalizeMediaPath(path, null);
    }

    /**
     * Normalize media path with optional folder hint (e.g., "categories", "products", "seller_documents").
     * If folder is provided and path is a bare filename, it will be placed under that folder.
     */
    public String normalizeMediaPath(String path, String folder) {
        String normalized = path.replace('\\', '/').trim();
        if (normalized.isBlank()) {
            return "";
        }
        normalized = normalized.replaceFirst("(?i)^(ads|pads)/products/", "uploads/products/");
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            try {
                return new java.net.URI(normalized).getPath();
            } catch (Exception ignored) {
                return normalized;
            }
        }
        if (normalized.startsWith("/") && !normalized.startsWith("/uploads/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("/")) {
            return rewriteLegacySellerDocumentPath(normalized.substring(1));
        }
        if (normalized.startsWith("uploads/sellers/")) {
            String fileName = normalized.substring("uploads/sellers/".length());
            if (isSellerDocumentFileName(fileName)) {
                return "/uploads/seller_documents/" + fileName;
            }
            return "/" + normalized;
        }
        if (normalized.startsWith("uploads/seller_documents/")) {
            return "/" + normalized;
        }
        if (normalized.startsWith("uploads/")) {
            return "/" + normalized;
        }
        if (isSellerDocumentFileName(normalized)) {
            return "/uploads/seller_documents/" + normalized;
        }
        if (!normalized.contains("/")) {
            if (folder != null && !folder.isBlank()) {
                return "/uploads/" + folder + "/" + normalized;
            }
            return "/uploads/sellers/" + normalized;
        }
        return "/" + normalized;
    }

    public boolean isSellerDocumentFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        String base = fileName.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        return SELLER_DOCUMENT_FILE.matcher(base).find();
    }

    public String sellerDocumentPublicPath(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String base = fileName.replace('\\', '/').trim();
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        return "/uploads/seller_documents/" + base;
    }

    private String rewriteLegacySellerDocumentPath(String pathWithoutLeadingSlash) {
        if (pathWithoutLeadingSlash.startsWith("uploads/sellers/")) {
            String fileName = pathWithoutLeadingSlash.substring("uploads/sellers/".length());
            if (isSellerDocumentFileName(fileName)) {
                return "/uploads/seller_documents/" + fileName;
            }
        }
        return "/" + pathWithoutLeadingSlash;
    }
}
