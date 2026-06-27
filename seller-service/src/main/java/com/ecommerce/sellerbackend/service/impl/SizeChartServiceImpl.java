package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.SizeChartRequest;
import com.ecommerce.sellerbackend.dto.SizeChartResponse;
import com.ecommerce.sellerbackend.dto.SizeChartRowRequest;
import com.ecommerce.sellerbackend.dto.SizeChartRowResponse;
import com.ecommerce.sellerbackend.entity.SizeChart;
import com.ecommerce.sellerbackend.exception.DuplicateResourceException;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.SizeChartRepository;
import com.ecommerce.sellerbackend.service.ProductMediaStorageService;
import com.ecommerce.sellerbackend.service.SizeChartService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SizeChartServiceImpl implements SizeChartService {

    private final SizeChartRepository sizeChartRepository;
    private final ProductMediaStorageService productMediaStorageService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SizeChartResponse> listForSeller(Long sellerId) {
        return sizeChartRepository.findBySellerIdAndIsActiveTrueOrderByUpdatedAtDesc(sellerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SizeChartResponse getForSeller(Long sellerId, Integer id) {
        return toResponse(getOwnedChart(sellerId, id));
    }

    @Override
    @Transactional
    public SizeChartResponse create(Long sellerId, SizeChartRequest request) {
        String name = request.getName().trim();
        if (sizeChartRepository.findBySellerIdAndIsActiveTrueOrderByUpdatedAtDesc(sellerId).stream()
                .anyMatch(c -> c.getChartName().equalsIgnoreCase(name))) {
            throw new DuplicateResourceException("A size chart named \"" + name + "\" already exists.");
        }

        SizeChart chart = new SizeChart();
        chart.setSellerId(sellerId);
        chart.setChartName(name);
        applyRequest(chart, request);
        return toResponse(sizeChartRepository.save(chart));
    }

    @Override
    @Transactional
    public SizeChartResponse update(Long sellerId, Integer id, SizeChartRequest request) {
        SizeChart chart = getOwnedChart(sellerId, id);
        String name = request.getName().trim();
        if (!chart.getChartName().equalsIgnoreCase(name)
                && sizeChartRepository.findBySellerIdAndIsActiveTrueOrderByUpdatedAtDesc(sellerId).stream()
                        .anyMatch(c -> !c.getId().equals(id) && c.getChartName().equalsIgnoreCase(name))) {
            throw new DuplicateResourceException("A size chart named \"" + name + "\" already exists.");
        }
        chart.setChartName(name);
        applyRequest(chart, request);
        return toResponse(sizeChartRepository.save(chart));
    }

    private void applyRequest(SizeChart chart, SizeChartRequest request) {
        chart.setCategoryId(request.getCategoryId());
        chart.setSubcategoryId(request.getSubcategoryId());
        chart.setChartData(buildChartDataJson(request));
        if (request.getImageSource() != null && !request.getImageSource().isBlank()) {
            chart.setChartImage(productMediaStorageService.storeSizeChartImage(
                    request.getImageSource().trim(), chart.getSellerId()));
        }
    }

    private String buildChartDataJson(SizeChartRequest request) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            String unit = normalizeUnit(request.getUnit());
            root.put("measurement_unit", unit);
            root.put("additional_notes", request.getNotes() != null ? request.getNotes().trim() : "");
            root.put("note", request.getNotes() != null ? request.getNotes().trim() : "");
            if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
                root.put("category_name", request.getCategoryName().trim());
            }
            if (request.getCategorySubName() != null && !request.getCategorySubName().isBlank()) {
                root.put("category_sub_name", request.getCategorySubName().trim());
            }
            if (request.getSubcategoryName() != null && !request.getSubcategoryName().isBlank()) {
                root.put("subcategory_name", request.getSubcategoryName().trim());
            }

            ArrayNode measurements = root.putArray("measurements");
            ArrayNode sizes = root.putArray("sizes");
            for (SizeChartRowRequest row : request.getRows()) {
                if (row.getSize() == null || row.getSize().isBlank()) {
                    continue;
                }
                ObjectNode measurement = measurements.addObject();
                measurement.put("size_name", row.getSize().trim());
                measurement.put("chest_bust", safe(row.getChest()));
                measurement.put("waist", safe(row.getWaist()));
                measurement.put("hip", safe(row.getHip()));
                measurement.put("length", safe(row.getLength()));
                measurement.put("sleeve", safe(row.getSleeve()));

                ObjectNode sizeRow = sizes.addObject();
                sizeRow.put("size", row.getSize().trim());
                sizeRow.put("chest", safe(row.getChest()));
                sizeRow.put("waist", safe(row.getWaist()));
                sizeRow.put("hip", safe(row.getHip()));
                sizeRow.put("length", safe(row.getLength()));
                sizeRow.put("sleeve", safe(row.getSleeve()));
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to build size chart data.");
        }
    }

    private SizeChartResponse toResponse(SizeChart chart) {
        ParsedChart parsed = parseChartData(chart.getChartData());
        return SizeChartResponse.builder()
                .id(chart.getId())
                .name(chart.getChartName())
                .categoryId(chart.getCategoryId())
                .subcategoryId(chart.getSubcategoryId())
                .categoryName(parsed.categoryName)
                .categorySubName(parsed.categorySubName)
                .subcategoryName(parsed.subcategoryName)
                .unit(parsed.unit)
                .notes(parsed.notes)
                .imageUrl(chart.getChartImage())
                .rows(parsed.rows)
                .build();
    }

    private ParsedChart parseChartData(String chartData) {
        ParsedChart parsed = new ParsedChart();
        if (chartData == null || chartData.isBlank()) {
            return parsed;
        }
        try {
            JsonNode root = objectMapper.readTree(chartData);
            parsed.unit = firstNonBlank(
                    text(root, "measurement_unit"),
                    text(root, "measurements"),
                    "cm");
            parsed.notes = firstNonBlank(text(root, "additional_notes"), text(root, "note"));
            parsed.categoryName = text(root, "category_name");
            parsed.categorySubName = text(root, "category_sub_name");
            parsed.subcategoryName = text(root, "subcategory_name");

            JsonNode measurements = root.path("measurements");
            if (measurements.isArray() && !measurements.isEmpty() && measurements.get(0).has("size_name")) {
                for (JsonNode row : measurements) {
                    parsed.rows.add(SizeChartRowResponse.builder()
                            .size(text(row, "size_name"))
                            .chest(text(row, "chest_bust"))
                            .waist(text(row, "waist"))
                            .hip(text(row, "hip"))
                            .length(text(row, "length"))
                            .sleeve(text(row, "sleeve"))
                            .build());
                }
                return parsed;
            }

            JsonNode sizes = root.path("sizes");
            if (sizes.isArray()) {
                for (JsonNode row : sizes) {
                    parsed.rows.add(SizeChartRowResponse.builder()
                            .size(text(row, "size"))
                            .chest(text(row, "chest"))
                            .waist(text(row, "waist"))
                            .hip(text(row, "hip"))
                            .length(text(row, "length"))
                            .sleeve(text(row, "sleeve"))
                            .build());
                }
            }
        } catch (Exception ignored) {
            // return empty parsed chart
        }
        return parsed;
    }

    private SizeChart getOwnedChart(Long sellerId, Integer id) {
        return sizeChartRepository.findByIdAndSellerId(id, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Size chart not found."));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.asText("").trim();
    }

    private static String safe(String value) {
        return value != null ? value.trim() : "";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String normalizeUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return "cm";
        }
        String lower = unit.toLowerCase(Locale.ROOT);
        if (lower.contains("inch")) {
            return "inches";
        }
        return "cm";
    }

    private static final class ParsedChart {
        private String unit = "cm";
        private String notes = "";
        private String categoryName = "";
        private String categorySubName = "";
        private String subcategoryName = "";
        private final List<SizeChartRowResponse> rows = new ArrayList<>();
    }
}
