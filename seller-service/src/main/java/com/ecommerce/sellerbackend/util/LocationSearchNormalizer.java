package com.ecommerce.sellerbackend.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Expands location search text with common alternate spellings and legacy names
 * so users find results even when they type differently.
 */
public final class LocationSearchNormalizer {

    private static final Map<String, List<String>> ALIASES = Map.ofEntries(
            Map.entry("bangalore", List.of("bengaluru")),
            Map.entry("bengaluru", List.of("bangalore")),
            Map.entry("bombay", List.of("mumbai")),
            Map.entry("mumbai", List.of("bombay")),
            Map.entry("calcutta", List.of("kolkata")),
            Map.entry("kolkata", List.of("calcutta")),
            Map.entry("madras", List.of("chennai")),
            Map.entry("chennai", List.of("madras")),
            Map.entry("hydrabad", List.of("hyderabad")),
            Map.entry("hyderbad", List.of("hyderabad")),
            Map.entry("hyderabad", List.of("hydrabad")),
            Map.entry("gurgaon", List.of("gurugram")),
            Map.entry("gurugram", List.of("gurgaon")),
            Map.entry("trivandrum", List.of("thiruvananthapuram")),
            Map.entry("thiruvananthapuram", List.of("trivandrum")),
            Map.entry("pondicherry", List.of("puducherry")),
            Map.entry("puducherry", List.of("pondicherry")),
            Map.entry("benares", List.of("varanasi")),
            Map.entry("varanasi", List.of("benares", "banaras")),
            Map.entry("banaras", List.of("varanasi")),
            Map.entry("baroda", List.of("vadodara")),
            Map.entry("vadodara", List.of("baroda")),
            Map.entry("cochin", List.of("kochi")),
            Map.entry("kochi", List.of("cochin")),
            Map.entry("mangalore", List.of("mangaluru")),
            Map.entry("mangaluru", List.of("mangalore")),
            Map.entry("mysore", List.of("mysuru")),
            Map.entry("mysuru", List.of("mysore")),
            Map.entry("vizianagaram", List.of("vizag")),
            Map.entry("vizag", List.of("visakhapatnam", "vizianagaram")),
            Map.entry("visakhapatnam", List.of("vizag")),
            Map.entry("delhi", List.of("new delhi")),
            Map.entry("newdelhi", List.of("new delhi", "delhi")),
            Map.entry("bengalaru", List.of("bengaluru", "bangalore")),
            Map.entry("banglore", List.of("bangalore", "bengaluru")),
            Map.entry("tamilnadu", List.of("tamil nadu")),
            Map.entry("tamil nadu", List.of("tamilnadu")),
            Map.entry("andhrapradesh", List.of("andhra pradesh")),
            Map.entry("andhra pradesh", List.of("andhrapradesh")),
            Map.entry("telengana", List.of("telangana")),
            Map.entry("telangana", List.of("telengana")),
            Map.entry("maharastra", List.of("maharashtra")),
            Map.entry("maharashtra", List.of("maharastra")),
            Map.entry("india", List.of("bharat")),
            Map.entry("bharat", List.of("india"))
    );

    private LocationSearchNormalizer() {
    }

    public static List<String> expandTerms(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        terms.add(normalized);
        terms.add(normalized.replace(" ", ""));

        addAliases(terms, normalized);
        addAliases(terms, normalized.replace(" ", ""));

        for (String word : normalized.split("\\s+")) {
            if (word.length() < 2) {
                continue;
            }
            terms.add(word);
            addAliases(terms, word);
        }

        String primary = LocationFuzzyMatcher.primaryWord(normalized);
        if (primary.length() >= 4) {
            for (String variant : LocationFuzzyMatcher.typoVariants(primary)) {
                terms.add(variant);
                if (terms.size() >= 18) {
                    break;
                }
            }
        }
        if (!normalized.contains(" ") && normalized.length() >= 4) {
            terms.addAll(LocationFuzzyMatcher.prefixTerms(normalized));
        }

        List<String> result = new ArrayList<>(terms);
        return result.subList(0, Math.min(16, result.size()));
    }

    private static void addAliases(LinkedHashSet<String> terms, String key) {
        List<String> aliases = ALIASES.get(key);
        if (aliases != null) {
            terms.addAll(aliases);
        }
    }
}
