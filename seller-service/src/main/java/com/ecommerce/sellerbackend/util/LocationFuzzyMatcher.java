package com.ecommerce.sellerbackend.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Fuzzy matching for location search — tolerates typos and ranks suggestions by similarity.
 */
public final class LocationFuzzyMatcher {

    private static final double MIN_SCORE_LONG_QUERY = 0.52;
    private static final double MIN_SCORE_SHORT_QUERY = 0.38;

    private LocationFuzzyMatcher() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public static double similarity(String left, String right) {
        String a = normalize(left);
        String b = normalize(right);
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        if (a.equals(b)) {
            return 1.0;
        }
        if (a.startsWith(b) || b.startsWith(a)) {
            int maxLen = Math.max(a.length(), b.length());
            int minLen = Math.min(a.length(), b.length());
            return 0.82 + (0.18 * minLen / maxLen);
        }
        int distance = levenshtein(a, b);
        int maxLen = Math.max(a.length(), b.length());
        return 1.0 - ((double) distance / maxLen);
    }

    /** Edit-distance-1 style variants for common single-character mistakes. */
    public static List<String> typoVariants(String word) {
        if (word == null || word.isBlank()) {
            return List.of();
        }
        String w = word.trim().toLowerCase(Locale.ROOT);
        if (w.length() < 4 || w.length() > 24) {
            return List.of();
        }

        LinkedHashSet<String> variants = new LinkedHashSet<>();
        for (int i = 0; i < w.length(); i++) {
            variants.add(w.substring(0, i) + w.substring(i + 1));
        }
        for (int i = 0; i < w.length() - 1; i++) {
            if (w.charAt(i) != w.charAt(i + 1)) {
                variants.add(w.substring(0, i) + w.charAt(i + 1) + w.charAt(i) + w.substring(i + 2));
            }
        }
        for (int i = 0; i < w.length() - 1; i++) {
            if (w.charAt(i) == w.charAt(i + 1)) {
                variants.add(w.substring(0, i) + w.substring(i + 1));
            }
        }
        variants.add(w + w.charAt(w.length() - 1));

        List<String> result = new ArrayList<>(variants);
        result.remove(w);
        return result.subList(0, Math.min(12, result.size()));
    }

    /** Short prefixes for India Post partial name lookup (e.g. cherla → Cherlagudipadu). */
    public static List<String> prefixTerms(String query) {
        String normalized = normalize(query);
        List<String> prefixes = new ArrayList<>();
        if (normalized.length() < 4) {
            return prefixes;
        }
        int maxPrefix = Math.min(6, normalized.length() - 1);
        for (int len = 4; len <= maxPrefix; len++) {
            prefixes.add(normalized.substring(0, len));
        }
        return prefixes;
    }

    public static double bestMatchScore(String query, String area, String city, String state, String displayName) {
        double best = 0;
        if (area != null && !area.isBlank()) {
            best = Math.max(best, similarity(query, area));
        }
        if (city != null && !city.isBlank()) {
            best = Math.max(best, similarity(query, city) * 0.92);
        }
        if (displayName != null && !displayName.isBlank()) {
            for (String part : displayName.split(",")) {
                String trimmed = part.trim();
                if (trimmed.length() >= 3) {
                    best = Math.max(best, similarity(query, trimmed));
                }
            }
        }
        if (state != null && !state.isBlank()) {
            best = Math.max(best, similarity(query, state) * 0.75);
        }
        return best;
    }

    public static boolean passesRelevanceFilter(String query, String area, String city, String displayName) {
        String q = normalize(query);
        if (q.length() < 5) {
            return true;
        }
        double score = bestMatchScore(query, area, city, null, displayName);
        double threshold = q.length() >= 8 ? MIN_SCORE_LONG_QUERY : MIN_SCORE_SHORT_QUERY;
        if (score >= threshold) {
            return true;
        }
        String areaNorm = normalize(area);
        return !areaNorm.isEmpty() && (areaNorm.startsWith(q) || q.startsWith(areaNorm));
    }

    public static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }

    public static String primaryWord(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        return java.util.Arrays.stream(query.trim().split("\\s+"))
                .max(Comparator.comparingInt(String::length))
                .orElse(query.trim());
    }
}
