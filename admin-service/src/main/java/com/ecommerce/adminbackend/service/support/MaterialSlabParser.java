package com.ecommerce.adminbackend.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses subcategory {@code material_slabs} JSON into material / HSN / GST options
 * (same rules as seller-service MaterialSlabParser).
 */
@Slf4j
public final class MaterialSlabParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MaterialSlabParser() {}

    public static List<Map<String, Object>> parse(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = MAPPER.readTree(rawJson);
            if (!root.isArray()) {
                return List.of();
            }
            List<Map<String, Object>> options = new ArrayList<>();
            for (JsonNode node : root) {
                String material = text(node, "material");
                if (material.isBlank()) {
                    continue;
                }
                String hsn = text(node, "hsn_code");
                if (hsn.isBlank()) {
                    hsn = text(node, "hsnCode");
                }
                Map<String, Object> option = new LinkedHashMap<>();
                option.put("material", material);
                option.put("hsnCode", hsn.isBlank() ? null : hsn);
                option.put("gst", resolveGst(node));
                options.add(option);
            }
            return options;
        } catch (Exception ex) {
            log.debug("Failed to parse material_slabs JSON: {}", ex.getMessage());
            return List.of();
        }
    }

    private static BigDecimal resolveGst(JsonNode node) {
        JsonNode slabs = node.get("gst_slabs");
        if (slabs != null && slabs.isArray() && !slabs.isEmpty()) {
            JsonNode first = slabs.get(0);
            if (first != null && first.has("gst") && !first.get("gst").isNull()) {
                return first.get("gst").decimalValue();
            }
        }
        if (node.has("gst") && !node.get("gst").isNull()) {
            return node.get("gst").decimalValue();
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText("").trim() : "";
    }
}
