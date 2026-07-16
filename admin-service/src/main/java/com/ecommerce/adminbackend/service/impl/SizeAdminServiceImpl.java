package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.Size;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SizeAdminServiceImpl extends BaseAdminService implements SizeAdminService {

    private static final DateTimeFormatter CREATED_FMT =
            DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH);
    private static final String UNASSIGNED = "Unassigned";

    private final SizeRepository sizeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        List<Size> sizes = sizeRepository.findAllByOrderBySizeNameAsc();
        Map<Long, LinkedHashSet<String>> categoriesBySizeId = loadCategoriesBySizeId();

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
        Map<Long, LinkedHashSet<String>> categoriesBySizeId = loadCategoriesBySizeId();
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

    private Map<Long, LinkedHashSet<String>> loadCategoriesBySizeId() {
        Map<Long, LinkedHashSet<String>> map = new LinkedHashMap<>();
        for (Object[] row : sizeRepository.findSizeMainCategoryPairs()) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            Long sizeId = ((Number) row[0]).longValue();
            String categoryName = String.valueOf(row[1]).trim();
            if (categoryName.isEmpty()) {
                continue;
            }
            map.computeIfAbsent(sizeId, ignored -> new LinkedHashSet<>()).add(categoryName);
        }
        return map;
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
