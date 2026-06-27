package com.ecommerce.authdemo.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Loads size charts from existing {@code size_charts.chart_data} (seller panel formats)
 * and normalizes for product detail API consumers.
 */
public final class SizeChartUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SizeChartUtil() {
    }

    public record NormalizedSizeChart(String unit, String note, List<List<String>> rows) {
    }

    /** Normalize DB {@code chart_data} → API JSON with headers + rows. */
    public static String normalizeDbChartDataToApiJson(String chartData) {
        NormalizedSizeChart normalized = normalizeFromDbChartData(chartData);
        if (normalized == null || normalized.rows() == null || normalized.rows().isEmpty()) {
            return null;
        }
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("unit", normalized.unit() != null ? normalized.unit() : "inches");
            if (normalized.note() != null && !normalized.note().isBlank()) {
                root.put("note", normalized.note());
            }
            ArrayNode headers = MAPPER.createArrayNode();
            for (String h : normalized.rows().get(0)) {
                headers.add(h);
            }
            root.set("headers", headers);
            ArrayNode rows = MAPPER.createArrayNode();
            for (int i = 1; i < normalized.rows().size(); i++) {
                ArrayNode row = MAPPER.createArrayNode();
                for (String cell : normalized.rows().get(i)) {
                    row.add(cell);
                }
                rows.add(row);
            }
            root.set("rows", rows);
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return chartData;
        }
    }

    public static NormalizedSizeChart normalizeFromDbChartData(String chartData) {
        if (chartData == null || chartData.isBlank()) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(chartData.trim());
            if (root == null || root.isNull()) {
                return null;
            }
            if (root.has("headers") && root.has("rows")) {
                return parseHeadersRowsFormat(root);
            }
            if (root.has("sizes") && root.get("sizes").isArray()) {
                return parseSizesFormat(root);
            }
            if (root.has("measurements") && root.get("measurements").isArray()) {
                return parseMeasurementsFormat(root);
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    public static String resolveSizeChartUnit(String sizeChartJson) {
        if (sizeChartJson == null || sizeChartJson.isBlank()) {
            return "inches";
        }
        try {
            JsonNode root = MAPPER.readTree(sizeChartJson);
            for (String key : List.of("unit", "measurements", "measurement_unit")) {
                JsonNode unit = root.get(key);
                if (unit != null && unit.isTextual() && !unit.asText().isBlank()) {
                    return unit.asText().trim();
                }
            }
        } catch (Exception ignored) {
            // default
        }
        return "inches";
    }

    public static String extractFromSpecifications(String specifications) {
        if (specifications == null || specifications.isBlank()) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(specifications.trim());
            if (root == null || root.isNull()) {
                return null;
            }
            if (root.has("sizeChart") && !root.get("sizeChart").isNull()) {
                return normalizeOrRaw(root.get("sizeChart"));
            }
            if (root.has("size_chart") && !root.get("size_chart").isNull()) {
                return normalizeOrRaw(root.get("size_chart"));
            }
            if (root.has("headers") && root.has("rows")) {
                return root.toString();
            }
            if (root.isArray()) {
                for (JsonNode item : root) {
                    if (item == null || !item.isObject()) {
                        continue;
                    }
                    String name = "";
                    if (item.has("name")) {
                        name = item.get("name").asText("").toLowerCase(Locale.ROOT);
                    } else if (item.has("key")) {
                        name = item.get("key").asText("").toLowerCase(Locale.ROOT);
                    }
                    if (name.contains("size chart") || "sizechart".equals(name)) {
                        JsonNode value = item.get("value");
                        if (value != null && !value.isNull()) {
                            return normalizeOrRaw(value);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // not JSON
        }
        return null;
    }

    private static String normalizeOrRaw(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String raw = node.isTextual() ? node.asText() : node.toString();
        String normalized = normalizeDbChartDataToApiJson(raw);
        return normalized != null ? normalized : raw;
    }

    private static NormalizedSizeChart parseHeadersRowsFormat(JsonNode root) {
        String unit = textOrDefault(root, "unit", "inches");
        String note = textOrDefault(root, "note", "");
        List<List<String>> rows = new ArrayList<>();
        JsonNode headers = root.get("headers");
        JsonNode body = root.get("rows");
        if (headers == null || !headers.isArray() || body == null || !body.isArray()) {
            return null;
        }
        List<String> headerRow = new ArrayList<>();
        headers.forEach(h -> headerRow.add(h.asText("")));
        rows.add(headerRow);
        body.forEach(rowNode -> {
            List<String> row = new ArrayList<>();
            if (rowNode.isArray()) {
                rowNode.forEach(cell -> row.add(cell.asText("")));
            }
            if (!row.isEmpty()) {
                rows.add(row);
            }
        });
        return rows.size() > 1 ? new NormalizedSizeChart(unit, note, rows) : null;
    }

    private static NormalizedSizeChart parseSizesFormat(JsonNode root) {
        String unit = firstNonBlank(
                textOrNull(root, "measurements"),
                textOrNull(root, "measurement_unit"),
                "inches"
        );
        String note = firstNonBlank(
                textOrNull(root, "note"),
                textOrNull(root, "additional_notes"),
                ""
        );
        JsonNode sizes = root.get("sizes");
        if (sizes == null || !sizes.isArray() || sizes.isEmpty()) {
            return null;
        }
        Set<String> measureKeys = new LinkedHashSet<>();
        for (JsonNode sizeRow : sizes) {
            if (sizeRow != null && sizeRow.isObject()) {
                Iterator<String> fields = sizeRow.fieldNames();
                while (fields.hasNext()) {
                    String field = fields.next();
                    if (!"size".equalsIgnoreCase(field)) {
                        measureKeys.add(field);
                    }
                }
            }
        }
        List<String> headers = new ArrayList<>();
        headers.add("Size");
        for (String key : measureKeys) {
            headers.add(formatHeader(key));
        }
        List<List<String>> rows = new ArrayList<>();
        rows.add(headers);
        for (JsonNode sizeRow : sizes) {
            if (sizeRow == null || !sizeRow.isObject()) {
                continue;
            }
            List<String> row = new ArrayList<>();
            row.add(textOrDefault(sizeRow, "size", ""));
            for (String key : measureKeys) {
                row.add(textOrDefault(sizeRow, key, ""));
            }
            rows.add(row);
        }
        return rows.size() > 1 ? new NormalizedSizeChart(unit, note, rows) : null;
    }

    private static NormalizedSizeChart parseMeasurementsFormat(JsonNode root) {
        String unit = firstNonBlank(
                textOrNull(root, "measurement_unit"),
                textOrNull(root, "measurements"),
                "inches"
        );
        String note = firstNonBlank(
                textOrNull(root, "additional_notes"),
                textOrNull(root, "note"),
                ""
        );
        JsonNode measurements = root.get("measurements");
        if (measurements == null || !measurements.isArray() || measurements.isEmpty()) {
            return null;
        }
        Set<String> measureKeys = new LinkedHashSet<>();
        for (JsonNode row : measurements) {
            if (row != null && row.isObject()) {
                Iterator<String> fields = row.fieldNames();
                while (fields.hasNext()) {
                    String field = fields.next();
                    if (!"size_name".equalsIgnoreCase(field) && !"size".equalsIgnoreCase(field)) {
                        measureKeys.add(field);
                    }
                }
            }
        }
        List<String> headers = new ArrayList<>();
        headers.add("Size");
        for (String key : measureKeys) {
            headers.add(formatHeader(key));
        }
        List<List<String>> rows = new ArrayList<>();
        rows.add(headers);
        for (JsonNode m : measurements) {
            if (m == null || !m.isObject()) {
                continue;
            }
            List<String> row = new ArrayList<>();
            String sizeLabel = firstNonBlank(
                    textOrNull(m, "size_name"),
                    textOrNull(m, "size"),
                    ""
            );
            row.add(sizeLabel);
            for (String key : measureKeys) {
                row.add(textOrDefault(m, key, ""));
            }
            rows.add(row);
        }
        return rows.size() > 1 ? new NormalizedSizeChart(unit, note, rows) : null;
    }

    private static String formatHeader(String key) {
        String normalized = key.replace('_', ' ').trim();
        if (normalized.isEmpty()) {
            return key;
        }
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || !v.isTextual()) {
            return null;
        }
        String text = v.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private static String textOrDefault(JsonNode node, String field, String fallback) {
        String text = textOrNull(node, field);
        return text != null ? text : fallback;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "inches";
    }
}
