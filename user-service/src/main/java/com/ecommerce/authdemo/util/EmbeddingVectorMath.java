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
     * Absolute cosine floor — below this is noise (screenshots vs catalog).
     * Kept modest so near-duplicate catalog photos still match when CLIP scores
     * are soft (recompression / crop / different resolution).
     */
    public static final double MIN_CAMERA_SIMILARITY = 0.20;

    /**
     * Prefer keeping candidates at least this strong, or within a relative band
     * of the best hit when the best is only moderately confident.
     */
    public static final double STRONG_CAMERA_SIMILARITY = 0.28;

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
        if (scored.isEmpty()) {
            return List.of();
        }
        scored.sort(Comparator.comparingDouble(Map.Entry<Long, Double>::getValue).reversed());
        double best = scored.get(0).getValue();
        // Keep strong hits, or anything close to the best match (same garment, softer score).
        double keepFloor = Math.max(minSimilarity, Math.min(STRONG_CAMERA_SIMILARITY, best * 0.82));
        return scored.stream()
                .filter(entry -> entry.getValue() >= keepFloor)
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }
}
