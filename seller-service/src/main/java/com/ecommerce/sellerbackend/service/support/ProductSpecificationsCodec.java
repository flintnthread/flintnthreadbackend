package com.ecommerce.sellerbackend.service.support;

import com.ecommerce.sellerbackend.dto.CreateProductRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProductSpecificationsCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ProductSpecificationsCodec() {}

    @Getter
    @Builder
    public static class CustomizationData {
        private final boolean enabled;
        private final String title;
        private final String instructions;
        private final String leadDays;
        private final String charge;
        private final boolean allowPhoto;
        private final String imageLabel;
        private final boolean allowText;
        private final String textLabel;
    }

    public static String encode(CreateProductRequest request, String rawSpecificationsJson) {
        List<Map<String, String>> specs = parseSpecRows(rawSpecificationsJson);
        boolean hasCatalogLeaf = request.getSubcategoryName() != null && !request.getSubcategoryName().isBlank();
        if (!Boolean.TRUE.equals(request.getCustomized()) && specs.isEmpty() && !hasCatalogLeaf) {
            return rawSpecificationsJson;
        }

        ObjectNode root = MAPPER.createObjectNode();
        if (hasCatalogLeaf) {
            ObjectNode catalogNode = root.putObject("catalog");
            catalogNode.put("leafSubcategoryName", request.getSubcategoryName().trim());
        }
        if (Boolean.TRUE.equals(request.getCustomized())) {
            ObjectNode customization = root.putObject("customization");
            customization.put("enabled", true);
            putText(customization, "title", request.getCustomTitle());
            putText(customization, "instructions", request.getCustomInstructions());
            putText(customization, "leadDays", request.getCustomLeadDays());
            putText(customization, "charge", request.getCustomCharge());
            customization.put("allowPhoto", Boolean.TRUE.equals(request.getCustomAllowPhoto()));
            putText(customization, "imageLabel", request.getCustomImageLabel());
            customization.put("allowText", Boolean.TRUE.equals(request.getCustomAllowText()));
            putText(customization, "textLabel", request.getCustomTextLabel());
        }
        ArrayNode items = root.putArray("specs");
        for (Map<String, String> row : specs) {
            ObjectNode item = items.addObject();
            item.put("name", row.get("name"));
            item.put("value", row.get("value"));
        }
        try {
            return MAPPER.writeValueAsString(root);
        } catch (Exception ex) {
            return rawSpecificationsJson;
        }
    }

    public static CustomizationData parseCustomization(String json) {
        if (json == null || json.isBlank()) {
            return CustomizationData.builder().enabled(false).build();
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            if (!root.isObject() || !root.has("customization")) {
                return CustomizationData.builder().enabled(false).build();
            }
            JsonNode c = root.get("customization");
            return CustomizationData.builder()
                    .enabled(c.path("enabled").asBoolean(false))
                    .title(text(c, "title"))
                    .instructions(text(c, "instructions"))
                    .leadDays(text(c, "leadDays"))
                    .charge(text(c, "charge"))
                    .allowPhoto(c.path("allowPhoto").asBoolean(false))
                    .imageLabel(text(c, "imageLabel"))
                    .allowText(c.path("allowText").asBoolean(false))
                    .textLabel(text(c, "textLabel"))
                    .build();
        } catch (Exception ex) {
            return CustomizationData.builder().enabled(false).build();
        }
    }

    public static String parseLeafSubcategoryName(String json) {
        if (json == null || json.isBlank()) {
            return "";
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root.isObject() && root.has("catalog")) {
                return text(root.get("catalog"), "leafSubcategoryName");
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "";
    }

    public static List<Map<String, Object>> parseSpecRowsForDisplay(String json) {
        List<Map<String, String>> rows = parseSpecRows(json);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, String> row : rows) {
            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("name", row.get("name"));
            mapped.put("value", row.get("value"));
            out.add(mapped);
        }
        return out;
    }

    private static List<Map<String, String>> parseSpecRows(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root.isArray()) {
                return readSpecArray(root);
            }
            if (root.isObject() && root.has("specs") && root.get("specs").isArray()) {
                return readSpecArray(root.get("specs"));
            }
        } catch (Exception ignored) {
            // fall through
        }
        return List.of();
    }

    private static List<Map<String, String>> readSpecArray(JsonNode array) {
        List<Map<String, String>> specs = new ArrayList<>();
        for (JsonNode row : array) {
            String name = text(row, "name");
            if (name.isBlank()) {
                name = text(row, "label");
            }
            String value = text(row, "value");
            if (!name.isBlank() && !value.isBlank()) {
                Map<String, String> mapped = new LinkedHashMap<>();
                mapped.put("name", name);
                mapped.put("value", value);
                specs.add(mapped);
            }
        }
        return specs;
    }

    private static void putText(ObjectNode node, String field, String value) {
        if (value != null && !value.isBlank()) {
            node.put(field, value.trim());
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("").trim();
    }
}
