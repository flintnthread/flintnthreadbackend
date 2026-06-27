package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.DeliveryCharge;
import com.ecommerce.adminbackend.repository.DeliveryChargeRepository;
import com.ecommerce.adminbackend.service.DeliverySlabAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeliverySlabAdminServiceImpl extends BaseAdminService implements DeliverySlabAdminService {

    private final DeliveryChargeRepository chargeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSlabs() {
        return chargeRepository.findAllByOrderByWeightMinAscIdAsc().stream()
                .map(this::toSlab)
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> create(DeliveryCharge input) {
        validateSlab(input);
        LocalDateTime now = LocalDateTime.now();
        input.setId(null);
        if (input.getCreatedAt() == null) {
            input.setCreatedAt(now);
        }
        input.setUpdatedAt(now);
        if (input.getStatus() == null) {
            input.setStatus(true);
        }
        if (input.getCustom() == null) {
            input.setCustom(false);
        }
        return toSlab(chargeRepository.save(input));
    }

    @Override
    @Transactional
    public Map<String, Object> update(Integer id, DeliveryCharge input) {
        DeliveryCharge charge = requireSlab(id);
        if (input.getWeightSlab() != null) {
            charge.setWeightSlab(input.getWeightSlab());
        }
        if (input.getWeightMin() != null) {
            charge.setWeightMin(input.getWeightMin());
        }
        if (input.getWeightMax() != null) {
            charge.setWeightMax(input.getWeightMax());
        }
        if (input.getIntraCityCharge() != null) {
            charge.setIntraCityCharge(input.getIntraCityCharge());
        }
        if (input.getMetroMetroCharge() != null) {
            charge.setMetroMetroCharge(input.getMetroMetroCharge());
        }
        if (input.getCustom() != null) {
            charge.setCustom(input.getCustom());
        }
        if (input.getStatus() != null) {
            charge.setStatus(input.getStatus());
        }
        charge.setUpdatedAt(LocalDateTime.now());
        return toSlab(chargeRepository.save(charge));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        requireSlab(id);
        chargeRepository.deleteById(id);
    }

    private DeliveryCharge requireSlab(Integer id) {
        return requireFound(chargeRepository.findById(id), "Delivery charge not found.");
    }

    private void validateSlab(DeliveryCharge slab) {
        requireNonBlank(slab.getWeightSlab(), "Label");
        if (slab.getWeightMin() == null || slab.getWeightMax() == null) {
            throw new IllegalArgumentException("Weight range is required.");
        }
        if (!Boolean.TRUE.equals(slab.getCustom())
                && (slab.getIntraCityCharge() == null || slab.getMetroMetroCharge() == null)) {
            throw new IllegalArgumentException("Delivery charges are required.");
        }
        if (Boolean.TRUE.equals(slab.getCustom())) {
            if (slab.getIntraCityCharge() == null) {
                slab.setIntraCityCharge(java.math.BigDecimal.ZERO);
            }
            if (slab.getMetroMetroCharge() == null) {
                slab.setMetroMetroCharge(java.math.BigDecimal.ZERO);
            }
        }
    }

    private Map<String, Object> toSlab(DeliveryCharge charge) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", charge.getId());
        row.put("label", charge.getWeightSlab());
        row.put("minWeightKg", charge.getWeightMin());
        row.put("maxWeightKg", charge.getWeightMax());
        row.put("intraCityCharge", charge.getIntraCityCharge());
        row.put("metroMetroCharge", charge.getMetroMetroCharge());
        row.put("active", Boolean.TRUE.equals(charge.getStatus()));
        row.put("custom", Boolean.TRUE.equals(charge.getCustom()));
        row.put("sortOrder", charge.getId());
        return row;
    }
}
