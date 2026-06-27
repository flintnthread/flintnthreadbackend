package com.ecommerce.authdemo.util;

import com.ecommerce.authdemo.entity.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Maps shop-by-gender labels to main departments and product gender fields. */
public final class GenderBrowseHelper {

    private GenderBrowseHelper() {
    }

    public static String normalizeGenderLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String g = raw.trim().toLowerCase(Locale.ENGLISH);
        return switch (g) {
            case "man", "men", "mens", "menswear" -> "Men";
            case "woman", "women", "womens", "womenswear" -> "Women";
            case "boy", "boys" -> "Boys";
            case "girl", "girls" -> "Girls";
            case "kid", "kids", "kidswear", "children" -> "Kids";
            default -> raw.trim().substring(0, 1).toUpperCase(Locale.ENGLISH)
                    + raw.trim().substring(1).toLowerCase(Locale.ENGLISH);
        };
    }

    public static Long resolveMainCategoryId(String genderLabel, List<Category> mainCategories) {
        if (mainCategories == null || mainCategories.isEmpty()) {
            return null;
        }
        String normalized = normalizeGenderLabel(genderLabel);
        for (Category category : mainCategories) {
            if (category == null || category.getCategoryName() == null) {
                continue;
            }
            String name = category.getCategoryName().trim().toLowerCase(Locale.ENGLISH);
            if ("Women".equals(normalized) && name.contains("women")) {
                return category.getId();
            }
            if ("Men".equals(normalized) && name.contains("men") && !name.contains("women")) {
                return category.getId();
            }
            if (("Boys".equals(normalized) || "Girls".equals(normalized) || "Kids".equals(normalized))
                    && (name.contains("kid") || name.contains("child"))) {
                return category.getId();
            }
        }
        return null;
    }

    /** Boys/Girls narrow within Kids department; Women/Men use whole department. */
    public static boolean shouldFilterByProductGenderField(String genderLabel) {
        String normalized = normalizeGenderLabel(genderLabel);
        return "Boys".equals(normalized) || "Girls".equals(normalized);
    }

    public static List<String> genderFieldValues(String genderLabel) {
        String normalized = normalizeGenderLabel(genderLabel);
        List<String> values = new ArrayList<>();
        switch (normalized) {
            case "Men" -> {
                values.add("men");
                values.add("man");
            }
            case "Women" -> {
                values.add("women");
                values.add("woman");
            }
            case "Boys" -> {
                values.add("boys");
                values.add("boy");
            }
            case "Girls" -> {
                values.add("girls");
                values.add("girl");
            }
            case "Kids" -> {
                values.add("kids");
                values.add("kid");
                values.add("boys");
                values.add("boy");
                values.add("girls");
                values.add("girl");
            }
            default -> values.add(normalized.toLowerCase(Locale.ENGLISH));
        }
        return values;
    }
}
