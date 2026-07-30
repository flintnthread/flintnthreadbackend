package com.ecommerce.authdemo.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingVectorMathTest {

    @Test
    void parseCsv_and_cosine_identicalVectorsAreOne() {
        double[] a = EmbeddingVectorMath.parseCsv("1,0,0");
        double[] b = EmbeddingVectorMath.parseCsv("1, 0, 0");
        assertEquals(1.0, EmbeddingVectorMath.cosineSimilarity(a, b), 1e-9);
    }

    @Test
    void topSimilarProductIds_ranksClosestFirst() {
        double[] query = EmbeddingVectorMath.parseCsv("1,0,0");
        Map<Long, String> catalog = new LinkedHashMap<>();
        catalog.put(10L, "0,1,0");
        catalog.put(20L, "0.9,0.1,0");
        catalog.put(30L, "1,0,0");

        List<Long> ranked = EmbeddingVectorMath.topSimilarProductIds(query, catalog, 2, 0.0);
        assertEquals(List.of(30L, 20L), ranked);
    }

    @Test
    void topSimilarProductIds_respectsMinSimilarityFloor() {
        double[] query = EmbeddingVectorMath.parseCsv("1,0,0");
        Map<Long, String> catalog = new LinkedHashMap<>();
        catalog.put(10L, "0,1,0"); // similarity 0
        catalog.put(20L, "0.15,0.99,0"); // below absolute floor
        catalog.put(30L, "0.95,0.05,0"); // strong

        List<Long> ranked = EmbeddingVectorMath.topSimilarProductIds(
                query, catalog, 5, EmbeddingVectorMath.MIN_CAMERA_SIMILARITY);
        assertEquals(List.of(30L), ranked);
    }

    @Test
    void topSimilarProductIds_keepsModerateMatchesNearBest() {
        double[] query = EmbeddingVectorMath.parseCsv("1,0,0");
        Map<Long, String> catalog = new LinkedHashMap<>();
        catalog.put(10L, "0.22,0.1,0"); // above floor, near best band
        catalog.put(20L, "0.25,0.05,0"); // best

        List<Long> ranked = EmbeddingVectorMath.topSimilarProductIds(
                query, catalog, 5, EmbeddingVectorMath.MIN_CAMERA_SIMILARITY);
        assertEquals(2, ranked.size());
        assertEquals(20L, ranked.get(0));
        assertTrue(ranked.contains(10L));
    }

    @Test
    void parseCsv_rejectsGarbage() {
        assertEquals(0, EmbeddingVectorMath.parseCsv("1,abc,3").length);
        assertTrue(EmbeddingVectorMath.topSimilarProductIds(new double[]{1}, Map.of(), 5).isEmpty());
    }
}
