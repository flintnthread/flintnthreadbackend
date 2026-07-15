package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.ads.AdsType;
import com.ecommerce.adminbackend.repository.AdsTypeRepository;
import com.ecommerce.adminbackend.service.AdsTypeAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdsTypeAdminServiceImpl extends BaseAdminService implements AdsTypeAdminService {

    private final AdsTypeRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String search, String status) {
        return repository.search(blankToNull(search), blankToNull(status)).stream().map(this::toMap).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> get(Integer id) {
        return toMap(requireFound(repository.findById(id), "Ad type not found."));
    }

    @Override
    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        AdsType entity = new AdsType();
        apply(entity, body, true);
        return toMap(repository.save(entity));
    }

    @Override
    @Transactional
    public Map<String, Object> update(Integer id, Map<String, Object> body) {
        AdsType entity = requireFound(repository.findById(id), "Ad type not found.");
        apply(entity, body, false);
        return toMap(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        requireFound(repository.findById(id), "Ad type not found.");
        repository.deleteById(id);
    }

    private void apply(AdsType entity, Map<String, Object> body, boolean creating) {
        if (creating || body.containsKey("name")) {
            entity.setName(requireNonBlank(stringAt(body, "name"), "name"));
        }
        if (creating || body.containsKey("category")) {
            entity.setCategory(requireNonBlank(stringAt(body, "category"), "category"));
        }
        if (creating || body.containsKey("description")) {
            entity.setDescription(requireNonBlank(stringAt(body, "description"), "description"));
        }
        if (body.containsKey("specifications")) {
            entity.setSpecifications(stringAt(body, "specifications"));
        }
        if (body.containsKey("requirements")) {
            entity.setRequirements(stringAt(body, "requirements"));
        }
        if (body.containsKey("status") || creating) {
            String status = stringAt(body, "status");
            entity.setStatus(normalizeActiveStatus(status));
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

    private Map<String, Object> toMap(AdsType entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("name", entity.getName());
        row.put("category", entity.getCategory());
        row.put("description", entity.getDescription());
        row.put("specifications", entity.getSpecifications());
        row.put("requirements", entity.getRequirements());
        row.put("status", entity.getStatus());
        row.put("createdAt", entity.getCreatedAt());
        row.put("updatedAt", entity.getUpdatedAt());
        return row;
    }
}
