package com.ecommerce.authdemo.util;

import com.ecommerce.authdemo.repository.ColorRepository;
import com.ecommerce.authdemo.repository.SizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SizeColorMapper {

    private final SizeRepository sizeRepository;
    private final ColorRepository colorRepository;

    // Size ID to Name mappings
    private static final Map<String, String> SIZE_MAP = new HashMap<>();
    
    // Color ID to Name mappings  
    private static final Map<String, String> COLOR_MAP = new HashMap<>();

    static {
        // Initialize size mappings (adjust based on your actual size IDs)
        SIZE_MAP.put("1", "XS");
        SIZE_MAP.put("2", "S");
        SIZE_MAP.put("3", "M");
        SIZE_MAP.put("4", "L");
        SIZE_MAP.put("5", "XL");
        SIZE_MAP.put("6", "XXL");
        SIZE_MAP.put("7", "3XL");
        SIZE_MAP.put("8", "4XL");
        SIZE_MAP.put("15", "S");
        SIZE_MAP.put("16", "M");
        SIZE_MAP.put("17", "L");
        SIZE_MAP.put("18", "XL");
        SIZE_MAP.put("52", "L");
        
        // Initialize color mappings (adjust based on your actual color IDs)
        COLOR_MAP.put("1", "Red");
        COLOR_MAP.put("2", "Blue");
        COLOR_MAP.put("3", "Green");
        COLOR_MAP.put("4", "Black");
        COLOR_MAP.put("5", "White");
        COLOR_MAP.put("6", "Yellow");
        COLOR_MAP.put("7", "Pink");
        COLOR_MAP.put("8", "Purple");
        COLOR_MAP.put("9", "Orange");
        COLOR_MAP.put("10", "Brown");
        COLOR_MAP.put("11", "Gray");
        COLOR_MAP.put("12", "Navy");
        COLOR_MAP.put("13", "Maroon");
        COLOR_MAP.put("14", "Beige");
        COLOR_MAP.put("15", "Cream");
        COLOR_MAP.put("16", "Yellow");
        COLOR_MAP.put("17", "Orange");
        COLOR_MAP.put("18", "Pink");
        COLOR_MAP.put("19", "Purple");
        COLOR_MAP.put("20", "Blue");
        COLOR_MAP.put("21", "Green");
        COLOR_MAP.put("22", "Navy");
        COLOR_MAP.put("23", "Sky Blue");
        COLOR_MAP.put("30", "Yellow");
        COLOR_MAP.put("32", "Cyan");
        COLOR_MAP.put("38", "Cream");
        COLOR_MAP.put("60", "White");
    }

    /** Normalize values like {@code "15.0"} to a whole-number id key for lookups. */
    private static String normalizeNumericIdKey(String trimmed) {
        try {
            return String.valueOf(new BigDecimal(trimmed).longValue());
        } catch (NumberFormatException e) {
            return trimmed;
        }
    }

    public String getSizeName(String sizeIdOrName) {
        if (sizeIdOrName == null || sizeIdOrName.trim().isEmpty()) {
            return sizeIdOrName;
        }
        String trimmed = sizeIdOrName.trim();

        if (trimmed.matches(".*[a-zA-Z].*")) {
            return trimmed;
        }

        String idKey = normalizeNumericIdKey(trimmed);
        try {
            long id = Long.parseLong(idKey);
            return sizeRepository.findById(id)
                    .map(s -> s.getName() != null && !s.getName().isBlank() ? s.getName().trim() : null)
                    .filter(s -> !s.isEmpty())
                    .orElse(SIZE_MAP.getOrDefault(idKey, SIZE_MAP.getOrDefault(trimmed, trimmed)));
        } catch (NumberFormatException ignored) {
            return SIZE_MAP.getOrDefault(trimmed, trimmed);
        }
    }

    public String getColorName(String colorIdOrName) {
        if (colorIdOrName == null || colorIdOrName.trim().isEmpty()) {
            return colorIdOrName;
        }
        String trimmed = colorIdOrName.trim();

        if (trimmed.matches(".*[a-zA-Z].*")) {
            return trimmed;
        }

        String idKey = normalizeNumericIdKey(trimmed);
        try {
            long id = Long.parseLong(idKey);
            return colorRepository.findById(id)
                    .map(c -> c.getName() != null && !c.getName().isBlank() ? c.getName().trim() : null)
                    .filter(s -> !s.isEmpty())
                    .orElse(COLOR_MAP.getOrDefault(idKey, COLOR_MAP.getOrDefault(trimmed, trimmed)));
        } catch (NumberFormatException ignored) {
            return COLOR_MAP.getOrDefault(trimmed, trimmed);
        }
    }

    // Add methods to update mappings dynamically if needed
    public void addSizeMapping(String id, String name) {
        SIZE_MAP.put(id, name);
    }

    public void addColorMapping(String id, String name) {
        COLOR_MAP.put(id, name);
    }
}
