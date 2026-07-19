package com.ecommerce.sellerbackend.util;

/**
 * Ensures public HTTPS links use hostnames covered by the live TLS certificate.
 * <p>
 * The VPS certificate SANs are {@code flintnthread.in} / {@code *.flintnthread.in} only.
 * Links to {@code flintnthread.online} cause browsers to show
 * {@code NET::ERR_CERT_COMMON_NAME_INVALID} (privacy error).
 */
public final class SslSafePublicUrl {

    private SslSafePublicUrl() {
    }

    public static String normalize(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String u = url.trim();
        // Longer hostnames first so bare domain replace does not leave a partial match.
        u = replaceHost(u, "https://www.flintnthread.online", "https://www.flintnthread.in");
        u = replaceHost(u, "https://seller.flintnthread.online", "https://seller.flintnthread.in");
        u = replaceHost(u, "https://admin.flintnthread.online", "https://admin.flintnthread.in");
        u = replaceHost(u, "https://flintnthread.online", "https://flintnthread.in");
        u = replaceHost(u, "http://www.flintnthread.online", "https://www.flintnthread.in");
        u = replaceHost(u, "http://seller.flintnthread.online", "https://seller.flintnthread.in");
        u = replaceHost(u, "http://admin.flintnthread.online", "https://admin.flintnthread.in");
        u = replaceHost(u, "http://flintnthread.online", "https://flintnthread.in");
        // Legacy path-based seller app → dedicated seller subdomain.
        u = replaceHost(u, "https://flintnthread.in/Seller", "https://seller.flintnthread.in");
        u = replaceHost(u, "http://flintnthread.in/Seller", "https://seller.flintnthread.in");
        return u;
    }

    private static String replaceHost(String url, String from, String to) {
        if (url.regionMatches(true, 0, from, 0, from.length())) {
            return to + url.substring(from.length());
        }
        return url.replace(from, to);
    }
}
