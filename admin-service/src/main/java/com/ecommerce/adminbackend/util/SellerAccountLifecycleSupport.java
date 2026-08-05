package com.ecommerce.adminbackend.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Mirrors seller-service lifecycle JSON helper — stores data in existing admin_remarks only. */
public final class SellerAccountLifecycleSupport {

    public static final String KEY = "accountLifecycle";
    public static final String DURATION_12H = "12h";
    public static final String DURATION_1D = "1d";
    public static final String TYPE_DEACTIVATION = "DEACTIVATION";
    public static final String TYPE_ACTIVATION = "ACTIVATION";
    public static final String REQ_PENDING = "PENDING";
    public static final String REQ_APPROVED = "APPROVED";
    public static final String REQ_REJECTED = "REJECTED";
    public static final String REQ_EXPIRED = "EXPIRED";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private SellerAccountLifecycleSupport() {
    }

    public static Map<String, Object> readLifecycle(String adminRemarks) {
        Map<String, Object> root = parseRoot(adminRemarks);
        Object lifecycle = root.get(KEY);
        if (lifecycle instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() != null) {
                    out.put(String.valueOf(e.getKey()), e.getValue());
                }
            }
            return out;
        }
        return new LinkedHashMap<>();
    }

    public static String writeLifecycle(String existingRemarks, Map<String, Object> lifecycle) {
        Map<String, Object> root = parseRoot(existingRemarks);
        if (lifecycle == null || lifecycle.isEmpty()) {
            root.remove(KEY);
        } else {
            root.put(KEY, lifecycle);
        }
        if (!root.containsKey("note") && existingRemarks != null && !existingRemarks.isBlank()
                && !existingRemarks.trim().startsWith("{")) {
            root.put("note", existingRemarks.trim());
        }
        try {
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize account lifecycle remarks.", e);
        }
    }

    public static LocalDateTime computeExpiresAt(LocalDateTime start, String duration) {
        if (start == null) {
            start = LocalDateTime.now();
        }
        String d = duration == null ? "" : duration.trim().toLowerCase(Locale.ENGLISH);
        if (DURATION_1D.equals(d) || "1day".equals(d) || "24h".equals(d)) {
            return start.plusDays(1);
        }
        return start.plusHours(12);
    }

    public static String normalizeDuration(String duration) {
        String d = duration == null ? "" : duration.trim().toLowerCase(Locale.ENGLISH);
        if (DURATION_1D.equals(d) || "1day".equals(d) || "1 day".equals(d) || "24h".equals(d)) {
            return DURATION_1D;
        }
        if (DURATION_12H.equals(d) || "12hours".equals(d) || "12 hours".equals(d)) {
            return DURATION_12H;
        }
        throw new IllegalArgumentException("Duration must be 12h or 1d.");
    }

    public static String format(LocalDateTime value) {
        return value == null ? null : ISO.format(value);
    }

    public static LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        try {
            return LocalDateTime.parse(s, ISO);
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(s);
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static String stringVal(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? null : s;
    }

    private static Map<String, Object> parseRoot(String adminRemarks) {
        if (adminRemarks == null || adminRemarks.isBlank()) {
            return new LinkedHashMap<>();
        }
        String trimmed = adminRemarks.trim();
        if (!trimmed.startsWith("{")) {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("note", trimmed);
            return root;
        }
        try {
            return MAPPER.readValue(trimmed, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("note", trimmed);
            return root;
        }
    }
}
