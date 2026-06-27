package com.ecommerce.sellerbackend.service.support;

import com.ecommerce.sellerbackend.dto.CatalogMaterialOptionResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class MaterialSlabParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MaterialSlabParser() {}

    public static List<CatalogMaterialOptionResponse> parse(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = MAPPER.readTree(rawJson);
            if (!root.isArray()) {
                return List.of();
            }
            List<CatalogMaterialOptionResponse> options = new ArrayList<>();
            for (JsonNode node : root) {
                String material = text(node, "material");
                if (material.isBlank()) {
                    continue;
                }
                String hsn = text(node, "hsn_code");
                if (hsn.isBlank()) {
                    hsn = text(node, "hsnCode");
                }
                BigDecimal gst = resolveGst(node);
                options.add(CatalogMaterialOptionResponse.builder()
                        .material(material)
                        .hsnCode(hsn)
                        .gst(gst)
                        .build());
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
