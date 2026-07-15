package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.ads.AdPlacement;
import com.ecommerce.adminbackend.repository.AdPlacementRepository;
import com.ecommerce.adminbackend.service.AdPlacementAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdPlacementAdminServiceImpl extends BaseAdminService implements AdPlacementAdminService {

    private final AdPlacementRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String search, String status) {
        return repository.search(blankToNull(search), blankToNull(status)).stream().map(this::toMap).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> get(Integer id) {
        return toMap(requireFound(repository.findById(id), "Ad placement not found."));
    }

    @Override
    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        AdPlacement entity = new AdPlacement();
        apply(entity, body, true);
        return toMap(repository.save(entity));
    }

    @Override
    @Transactional
    public Map<String, Object> update(Integer id, Map<String, Object> body) {
        AdPlacement entity = requireFound(repository.findById(id), "Ad placement not found.");
        apply(entity, body, false);
        return toMap(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        requireFound(repository.findById(id), "Ad placement not found.");
        repository.deleteById(id);
    }

    private void apply(AdPlacement entity, Map<String, Object> body, boolean creating) {
        if (creating || body.containsKey("name")) {
            entity.setName(requireNonBlank(stringAt(body, "name"), "name"));
        }
        if (creating || body.containsKey("type")) {
            entity.setType(requireNonBlank(stringAt(body, "type"), "type"));
        }
        if (creating || body.containsKey("dailyRate") || body.containsKey("daily_rate")) {
            entity.setDailyRate(requireDecimal(body, "dailyRate", "daily_rate", "dailyRate"));
        }
        if (creating || body.containsKey("monthlyRate") || body.containsKey("monthly_rate")) {
            entity.setMonthlyRate(requireDecimal(body, "monthlyRate", "monthly_rate", "monthlyRate"));
        }
        if (body.containsKey("description")) {
            entity.setDescription(stringAt(body, "description"));
        }
        if (body.containsKey("status") || creating) {
            entity.setStatus(normalizeActiveStatus(stringAt(body, "status")));
        }
    }

    private BigDecimal requireDecimal(Map<String, Object> body, String camel, String snake, String label) {
        Object value = body.containsKey(camel) ? body.get(camel) : body.get(snake);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be a number.");
        }
    }

    private String normalizeActiveStatus(String status) {
        if (status == null || status.isBlank()) {
            return "active";
        }
        String normalized = status.trim().toLowerCase();
        if (!"active".equals(normalized) && !"inactive".equals(normalized)) {
            throw new IllegalArgumentException("status must be active or inactive.");
        }
        return normalized;
    }

    private Map<String, Object> toMap(AdPlacement entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("name", entity.getName());
        row.put("type", entity.getType());
        row.put("dailyRate", entity.getDailyRate());
        row.put("monthlyRate", entity.getMonthlyRate());
        row.put("description", entity.getDescription());
        row.put("status", entity.getStatus());
        row.put("createdAt", entity.getCreatedAt());
        row.put("updatedAt", entity.getUpdatedAt());
        return row;
    }
}
