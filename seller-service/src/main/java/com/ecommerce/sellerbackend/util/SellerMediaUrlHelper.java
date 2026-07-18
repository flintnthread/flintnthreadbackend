package com.ecommerce.sellerbackend.util;

import java.util.regex.Pattern;

/**
 * Maps DB file names / legacy paths to public {@code /uploads/seller_documents/...} URLs.
 * Absolute URLs always use {@code https://flintnthread.com} (files are not on .in).
 */
public final class SellerMediaUrlHelper {

    private static final String MEDIA_CDN = "https://flintnthread.com";

    private static final Pattern SELLER_DOCUMENT_FILE = Pattern.compile(
            "^\\d+_(profile_pic|aadhar_front|aadhar_back|pan_card|business_proof|bank_proof|"
                    + "cancelled_cheque|live_selfie|company_pan_doc|incorporation_certificate|"
                    + "partnership_deed|msme_certificate|iec_certificate)(_|\\.)",
            Pattern.CASE_INSENSITIVE);

    private SellerMediaUrlHelper() {
    }

    public static boolean isSellerDocumentFileName(String fileName) {
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

    /**
     * Relative public path for seller profile pics / KYC docs stored on disk.
     * Absolute upload URLs are rewritten onto {@code /uploads/seller_documents/...} when needed.
     * Cloudinary URLs are returned unchanged.
     */
    public static String toPublicPath(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        String trimmed = stored.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            String lower = trimmed.toLowerCase();
            if (lower.contains("res.cloudinary.com/") || lower.contains("cloudinary.com/")) {
                return trimmed;
            }
            int idx = trimmed.indexOf("/uploads/");
            if (idx >= 0) {
                return toPublicPath(trimmed.substring(idx));
            }
            return trimmed;
        }

        String normalized = trimmed.replace('\\', '/');
        if (normalized.startsWith("/uploads/seller_documents/")) {
            return normalized;
        }
        if (normalized.startsWith("uploads/seller_documents/")) {
            return "/" + normalized;
        }
        if (normalized.startsWith("/uploads/sellers/")) {
            String fileName = normalized.substring("/uploads/sellers/".length());
            if (isSellerDocumentFileName(fileName)) {
                return "/uploads/seller_documents/" + fileName;
            }
            return normalized;
        }
        if (normalized.startsWith("uploads/sellers/")) {
            String fileName = normalized.substring("uploads/sellers/".length());
            if (isSellerDocumentFileName(fileName)) {
                return "/uploads/seller_documents/" + fileName;
            }
            return "/" + normalized;
        }
        if (normalized.startsWith("/uploads/")) {
            return normalized;
        }
        if (normalized.startsWith("uploads/")) {
            return "/" + normalized;
        }

        int slash = normalized.lastIndexOf('/');
        String fileName = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        if (isSellerDocumentFileName(fileName)) {
            return "/uploads/seller_documents/" + fileName;
        }
        return "/uploads/seller_documents/" + normalized;
    }

    public static String toAbsoluteUrl(String stored, String publicBaseUrl) {
        String path = toPublicPath(stored);
        if (path == null || path.isBlank()) {
            return null;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            String lower = path.toLowerCase();
            if (lower.contains("res.cloudinary.com/")) {
                return path;
            }
            int idx = path.indexOf("/uploads/");
            if (idx >= 0) {
                return MEDIA_CDN + path.substring(idx);
            }
            return path;
        }
        // Seller documents / uploads always on flintnthread.com
        if (path.contains("/uploads/")) {
            return MEDIA_CDN + (path.startsWith("/") ? path : "/" + path);
        }
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/$", "");
        if (base.isBlank()
                || base.contains("flintnthread.in")
                || base.contains("flintnthread.online")) {
            base = MEDIA_CDN;
        }
        return base + path;
    }
}
