package com.ecommerce.authdemo.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Pure helpers for CLIP embedding CSV vectors used by camera / visual search.
 */
public final class EmbeddingVectorMath {

    /**
     * Cosine floor for camera search. Below this, matches are noise
     * (e.g. screenshots / unrelated photos still score slightly &gt; 0).
     */
    public static final double MIN_CAMERA_SIMILARITY = 0.32;

    private EmbeddingVectorMath() {
    }

    public static double[] parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return new double[0];
        }
        String[] parts = csv.split(",");
        double[] vector = new double[parts.length];
        int length = 0;
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                vector[length++] = Double.parseDouble(trimmed);
            } catch (NumberFormatException ignored) {
                return new double[0];
            }
        }
        if (length == parts.length) {
            return vector;
        }
        double[] resized = new double[length];
        System.arraycopy(vector, 0, resized, 0, length);
        return resized;
    }

    public static double cosineSimilarity(double[] left, double[] right) {
        if (left == null || right == null || left.length == 0 || left.length != right.length) {
            return 0;
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    public static List<Long> topSimilarProductIds(
            double[] queryVector,
            Map<Long, String> productIdToEmbeddingCsv,
            int limit
    ) {
        return topSimilarProductIds(queryVector, productIdToEmbeddingCsv, limit, MIN_CAMERA_SIMILARITY);
    }

    public static List<Long> topSimilarProductIds(
            double[] queryVector,
            Map<Long, String> productIdToEmbeddingCsv,
            int limit,
            double minSimilarity
    ) {
        if (queryVector == null || queryVector.length == 0 || productIdToEmbeddingCsv == null || limit <= 0) {
            return List.of();
        }
        List<Map.Entry<Long, Double>> scored = new ArrayList<>();
        for (Map.Entry<Long, String> entry : productIdToEmbeddingCsv.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            double[] candidate = parseCsv(entry.getValue());
            if (candidate.length != queryVector.length) {
                continue;
            }
            double similarity = cosineSimilarity(queryVector, candidate);
            if (similarity >= minSimilarity) {
                scored.add(Map.entry(entry.getKey(), similarity));
            }
        }
        scored.sort(Comparator.comparingDouble(Map.Entry<Long, Double>::getValue).reversed());
        return scored.stream().limit(limit).map(Map.Entry::getKey).toList();
    }
}
