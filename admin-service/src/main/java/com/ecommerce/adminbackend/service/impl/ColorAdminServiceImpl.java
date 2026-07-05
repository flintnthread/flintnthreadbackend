package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.Color;
import com.ecommerce.adminbackend.repository.ColorRepository;
import com.ecommerce.adminbackend.service.ColorAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ColorAdminServiceImpl extends BaseAdminService implements ColorAdminService {

    private static final DateTimeFormatter CREATED_FMT =
            DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH);

    private final ColorRepository colorRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return colorRepository.findAllByOrderByColorNameAsc().stream()
                .map(this::toRow)
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> create(Map<String, Object> request) {
        Color color = new Color();
        color.setColorName(requireNonBlank(stringAt(request, "name"), "Color name"));
        color.setColorCode(requireNonBlank(stringAt(request, "code"), "Color code"));
        color.setStatus(parseStatus(request.get("status"), true));
        color.setSellerId(null);
        color.setCreatedAt(LocalDateTime.now());
        return toRow(colorRepository.save(color));
    }

    @Override
    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> request) {
        Color color = requireColor(id);
        if (request.containsKey("name")) {
            color.setColorName(requireNonBlank(stringAt(request, "name"), "Color name"));
        }
        if (request.containsKey("code")) {
            color.setColorCode(requireNonBlank(stringAt(request, "code"), "Color code"));
        }
        if (request.containsKey("status")) {
            color.setStatus(parseStatus(request.get("status"), color.getStatus()));
        }
        return toRow(colorRepository.save(color));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireColor(id);
        colorRepository.deleteById(id);
    }

    private Color requireColor(Long id) {
        return requireFound(colorRepository.findById(id), "Color not found.");
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

    private Map<String, Object> toRow(Color color) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", color.getId());
        row.put("name", color.getColorName());
        row.put("code", color.getColorCode());
        row.put("status", Boolean.TRUE.equals(color.getStatus()) ? "Active" : "Inactive");
        if (color.getCreatedAt() != null) {
            row.put("createdDate", color.getCreatedAt().format(CREATED_FMT));
        }
        return row;
    }
}
