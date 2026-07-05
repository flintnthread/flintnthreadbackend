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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SizeAdminServiceImpl extends BaseAdminService implements SizeAdminService {

    private static final DateTimeFormatter CREATED_FMT =
            DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH);

    private final SizeRepository sizeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return sizeRepository.findAllByOrderBySizeNameAsc().stream()
                .map(this::toRow)
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
        return toRow(sizeRepository.save(size));
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
        return toRow(sizeRepository.save(size));
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

    private Map<String, Object> toRow(Size size) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", size.getId());
        row.put("name", size.getSizeName());
        row.put("code", size.getSizeCode());
        row.put("status", Boolean.TRUE.equals(size.getStatus()) ? "Active" : "Inactive");
        if (size.getCreatedAt() != null) {
            row.put("createdDate", size.getCreatedAt().format(CREATED_FMT));
        }
        return row;
    }
}
