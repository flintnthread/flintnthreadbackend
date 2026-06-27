package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.BulkImportResponse;
import com.ecommerce.sellerbackend.dto.CreateProductRequest;
import com.ecommerce.sellerbackend.dto.CreateProductResponse;
import com.ecommerce.sellerbackend.dto.CreateProductVariantRequest;
import com.ecommerce.sellerbackend.repository.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class ProductBulkImportService {

    private final ProductCreateService productCreateService;
    private final SubcategoryRepository subcategoryRepository;

    @Transactional
    public BulkImportResponse importZip(Long sellerId, MultipartFile file) {
        List<String> errors = new ArrayList<>();
        List<Long> productIds = new ArrayList<>();
        int variantsCreated = 0;

        if (file == null || file.isEmpty()) {
            return BulkImportResponse.builder()
                    .productsCreated(0)
                    .variantsCreated(0)
                    .productIds(List.of())
                    .errors(List.of("ZIP file is required."))
                    .build();
        }

        try {
            Path tempDir = Files.createTempDirectory("bulk-import-" + sellerId);
            Map<String, byte[]> imagesByName = new HashMap<>();
            String csvContent = null;

            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    String name = entry.getName().replace('\\', '/');
                    byte[] bytes = zis.readAllBytes();
                    if (name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
                        csvContent = new String(bytes, StandardCharsets.UTF_8);
                    } else if (name.toLowerCase(Locale.ROOT).contains("/images/")
                            || name.toLowerCase(Locale.ROOT).startsWith("images/")) {
                        String fileName = name.substring(name.lastIndexOf('/') + 1);
                        imagesByName.put(fileName.toLowerCase(Locale.ROOT), bytes);
                    }
                }
            }

            if (csvContent == null || csvContent.isBlank()) {
                errors.add("products.csv not found in ZIP.");
                return BulkImportResponse.builder()
                        .productsCreated(0)
                        .variantsCreated(0)
                        .productIds(productIds)
                        .errors(errors)
                        .build();
            }

            Map<String, List<CsvRow>> grouped = parseCsv(csvContent);
            Integer defaultSubcategoryId = subcategoryRepository.findAll().stream()
                    .min(Comparator.comparing(s -> s.getSubcategoryName() != null ? s.getSubcategoryName() : ""))
                    .map(s -> s.getId())
                    .orElse(null);

            for (Map.Entry<String, List<CsvRow>> group : grouped.entrySet()) {
                try {
                    List<CsvRow> rows = group.getValue();
                    CsvRow first = rows.get(0);
                    CreateProductRequest request = new CreateProductRequest();
                    request.setName(first.title);
                    request.setShortDescription(truncate(first.description, 250));
                    request.setDescription(first.description != null ? first.description : first.title);
                    request.setHsnCode("61091000");
                    request.setProductMaterialType(first.productType != null ? first.productType : "Mixed Fabric");
                    request.setLengthCm(BigDecimal.TEN);
                    request.setWidthCm(BigDecimal.TEN);
                    request.setHeightCm(BigDecimal.ONE);
                    request.setProductWeight(BigDecimal.ONE);
                    request.setReturnPolicy("7 Days Return");
                    request.setSubcategoryId(defaultSubcategoryId);
                    if (defaultSubcategoryId != null) {
                        subcategoryRepository.findById(defaultSubcategoryId).ifPresent(sub -> {
                            request.setCategoryId(sub.getCategoryId());
                        });
                    }

                    List<CreateProductVariantRequest> variants = new ArrayList<>();
                    for (CsvRow row : rows) {
                        CreateProductVariantRequest variant = new CreateProductVariantRequest();
                        variant.setClientKey(row.sku != null ? row.sku : row.handle + "-" + row.option1Value);
                        variant.setColor(row.option1Value != null && !row.option1Value.isBlank()
                                ? row.option1Value : "Black");
                        variant.setSize(row.option2Value != null && !row.option2Value.isBlank()
                                ? row.option2Value : "Free Size");
                        variant.setSku(row.sku);
                        variant.setStock(row.inventory != null ? row.inventory : 0);
                        BigDecimal selling = row.price != null ? row.price : BigDecimal.ZERO;
                        BigDecimal mrp = row.compareAtPrice != null && row.compareAtPrice.compareTo(selling) > 0
                                ? row.compareAtPrice
                                : selling;
                        variant.setSellingPrice(selling);
                        variant.setMrp(mrp);
                        variants.add(variant);
                    }
                    request.setVariants(variants);

                    CreateProductResponse created = productCreateService.create(sellerId, request);
                    productIds.add(created.getProductId());
                    variantsCreated += variants.size();
                } catch (Exception ex) {
                    errors.add("Handle " + group.getKey() + ": " + ex.getMessage());
                }
            }

            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // ignore cleanup errors
                        }
                    });
        } catch (IOException ex) {
            errors.add("Failed to read ZIP: " + ex.getMessage());
        }

        return BulkImportResponse.builder()
                .productsCreated(productIds.size())
                .variantsCreated(variantsCreated)
                .productIds(productIds)
                .errors(errors)
                .build();
    }

    private Map<String, List<CsvRow>> parseCsv(String content) throws IOException {
        Map<String, List<CsvRow>> grouped = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new java.io.ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return grouped;
            }
            List<String> headers = parseCsvLine(headerLine);
            Map<String, Integer> index = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                index.put(headers.get(i).trim().toLowerCase(Locale.ROOT), i);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> cols = parseCsvLine(line);
                CsvRow row = new CsvRow();
                row.handle = col(cols, index, "product handle");
                row.title = col(cols, index, "title");
                row.description = col(cols, index, "description");
                row.price = decimal(col(cols, index, "price"));
                row.compareAtPrice = decimal(col(cols, index, "compare at price"));
                row.sku = col(cols, index, "sku");
                row.inventory = integer(col(cols, index, "inventory"));
                row.option1Value = col(cols, index, "option1 value");
                row.option2Value = col(cols, index, "option2 value");
                row.imageFilename = col(cols, index, "image filename");
                row.productType = col(cols, index, "product type");
                if (row.handle == null || row.handle.isBlank()) {
                    continue;
                }
                grouped.computeIfAbsent(row.handle, k -> new ArrayList<>()).add(row);
            }
        }
        return grouped;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());
        return result;
    }

    private static String col(List<String> cols, Map<String, Integer> index, String key) {
        Integer i = index.get(key);
        if (i == null || i >= cols.size()) {
            return null;
        }
        String v = cols.get(i);
        return v != null && !v.isBlank() ? v.trim() : null;
    }

    private static BigDecimal decimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Integer integer(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static class CsvRow {
        String handle;
        String title;
        String description;
        BigDecimal price;
        BigDecimal compareAtPrice;
        String sku;
        Integer inventory;
        String option1Value;
        String option2Value;
        String imageFilename;
        String productType;
    }
}
