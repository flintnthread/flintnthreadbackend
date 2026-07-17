package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.Category;
import com.ecommerce.adminbackend.entity.Product;
import com.ecommerce.adminbackend.entity.ProductVariant;
import com.ecommerce.adminbackend.entity.Size;
import com.ecommerce.adminbackend.repository.CategoryRepository;
import com.ecommerce.adminbackend.repository.ProductRepository;
import com.ecommerce.adminbackend.repository.ProductVariantRepository;
import com.ecommerce.adminbackend.repository.SizeRepository;
import com.ecommerce.adminbackend.service.SizeAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SizeAdminServiceImpl extends BaseAdminService implements SizeAdminService {

    private static final DateTimeFormatter CREATED_FMT =
            DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH);
    private static final String UNASSIGNED = "Unassigned";

    private final SizeRepository sizeRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        List<Size> sizes = sizeRepository.findAllByOrderBySizeNameAsc();
        Map<Long, LinkedHashSet<String>> categoriesBySizeId = loadCategoriesBySizeId(sizes);

        return sizes.stream()
                .map(size -> toRow(size, categoriesBySizeId.getOrDefault(size.getId(), new LinkedHashSet<>())))
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> create(Map<String, Object> request) {
        Size size = new Size();
        size.setSizeName(requireNonBlank(stringAt(request, "name"), "Size name"));
        size.setSizeCode(requireNonBlank(stringAt(request, "code"), "Size code"));
        size.setStatus(parseStatus(request.get("status"), true));
        size.setSellerId(null);
        size.setCreatedAt(LocalDateTime.now());
        return toRow(sizeRepository.save(size), new LinkedHashSet<>());
    }

    @Override
    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> request) {
        Size size = requireSize(id);
        if (request.containsKey("name")) {
            size.setSizeName(requireNonBlank(stringAt(request, "name"), "Size name"));
        }
        if (request.containsKey("code")) {
            size.setSizeCode(requireNonBlank(stringAt(request, "code"), "Size code"));
        }
        if (request.containsKey("status")) {
            size.setStatus(parseStatus(request.get("status"), size.getStatus()));
        }
        Size saved = sizeRepository.save(size);
        Map<Long, LinkedHashSet<String>> categoriesBySizeId =
                loadCategoriesBySizeId(sizeRepository.findAllByOrderBySizeNameAsc());
        return toRow(saved, categoriesBySizeId.getOrDefault(saved.getId(), new LinkedHashSet<>()));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireSize(id);
        sizeRepository.deleteById(id);
    }

    private Size requireSize(Long id) {
        return requireFound(sizeRepository.findById(id), "Size not found.");
    }

    private Map<Long, LinkedHashSet<String>> loadCategoriesBySizeId(List<Size> sizes) {
        try {
            return buildCategoriesBySizeId(sizes);
        } catch (Exception ex) {
            log.warn("Could not resolve size categories from products: {}", ex.getMessage());
            return Map.of();
        }
    }

    private Map<Long, LinkedHashSet<String>> buildCategoriesBySizeId(List<Size> sizes) {
        if (sizes == null || sizes.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> sizeIdByToken = new HashMap<>();
        for (Size size : sizes) {
            if (size.getId() != null) {
                sizeIdByToken.put(String.valueOf(size.getId()), size.getId());
            }
            if (size.getSizeName() != null && !size.getSizeName().isBlank()) {
                sizeIdByToken.put(size.getSizeName().trim().toLowerCase(Locale.ROOT), size.getId());
            }
            if (size.getSizeCode() != null && !size.getSizeCode().isBlank()) {
                sizeIdByToken.put(size.getSizeCode().trim().toLowerCase(Locale.ROOT), size.getId());
            }
        }

        List<ProductVariant> variants = productVariantRepository.findBySizeIsNotNull().stream()
                .filter(variant -> variant.getSize() != null && !variant.getSize().isBlank())
                .toList();
        if (variants.isEmpty()) {
            return Map.of();
        }

        Set<Long> productIds = variants.stream()
                .map(ProductVariant::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product, (left, right) -> left));

        Map<Integer, Category> categoriesById = new HashMap<>();
        Set<Integer> categoryIds = productsById.values().stream()
                .map(Product::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        categoryRepository.findAllById(categoryIds).forEach(category -> categoriesById.put(category.getId(), category));

        Set<Integer> parentIds = categoriesById.values().stream()
                .map(Category::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        categoryRepository.findAllById(parentIds).forEach(category -> categoriesById.putIfAbsent(category.getId(), category));

        Map<Long, LinkedHashSet<String>> map = new LinkedHashMap<>();
        for (ProductVariant variant : variants) {
            Long sizeId = resolveSizeId(variant.getSize(), sizeIdByToken);
            if (sizeId == null) {
                continue;
            }
            Product product = productsById.get(variant.getProductId());
            if (product == null || product.getCategoryId() == null) {
                continue;
            }
            String mainCategoryName = resolveMainCategoryName(product.getCategoryId(), categoriesById);
            if (mainCategoryName == null || mainCategoryName.isBlank()) {
                continue;
            }
            map.computeIfAbsent(sizeId, ignored -> new LinkedHashSet<>()).add(mainCategoryName);
        }
        return map;
    }

    private Long resolveSizeId(String raw, Map<String, Long> lookup) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String token = raw.trim();
        Long direct = lookup.get(token);
        if (direct != null) {
            return direct;
        }
        return lookup.get(token.toLowerCase(Locale.ROOT));
    }

    private String resolveMainCategoryName(Integer categoryId, Map<Integer, Category> categoriesById) {
        Category category = categoriesById.get(categoryId);
        if (category == null) {
            category = categoryRepository.findById(categoryId).orElse(null);
            if (category != null) {
                categoriesById.put(category.getId(), category);
            }
        }
        if (category == null) {
            return null;
        }
        if (category.getParentId() == null) {
            return category.getCategoryName();
        }
        Category parent = categoriesById.get(category.getParentId());
        if (parent == null) {
            parent = categoryRepository.findById(category.getParentId()).orElse(null);
            if (parent != null) {
                categoriesById.put(parent.getId(), parent);
            }
        }
        return parent != null ? parent.getCategoryName() : category.getCategoryName();
    }

    private boolean parseStatus(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return defaultValue;
        }
        return !"inactive".equalsIgnoreCase(text) && !"false".equalsIgnoreCase(text);
    }

    private Map<String, Object> toRow(Size size, Set<String> categoryNames) {
        List<String> categories = categoryNames == null || categoryNames.isEmpty()
                ? List.of()
                : categoryNames.stream()
                        .sorted(Comparator.comparing(String::toLowerCase))
                        .collect(Collectors.toCollection(ArrayList::new));

        String primaryCategory = categories.isEmpty() ? UNASSIGNED : categories.get(0);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", size.getId());
        row.put("name", size.getSizeName());
        row.put("code", size.getSizeCode());
        row.put("status", Boolean.TRUE.equals(size.getStatus()) ? "Active" : "Inactive");
        if (size.getCreatedAt() != null) {
            row.put("createdDate", size.getCreatedAt().format(CREATED_FMT));
        }
        row.put("categories", categories);
        row.put("primaryCategory", primaryCategory);
        return row;
    }
}
