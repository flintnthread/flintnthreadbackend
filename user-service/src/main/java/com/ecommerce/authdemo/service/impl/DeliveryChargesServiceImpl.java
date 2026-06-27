package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.DeliveryChargeRequest;
import com.ecommerce.authdemo.dto.DeliveryChargeResponse;
import com.ecommerce.authdemo.entity.DeliveryCharges;
import com.ecommerce.authdemo.exception.OrderException;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.DeliveryChargesRepository;
import com.ecommerce.authdemo.service.DeliveryChargesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryChargesServiceImpl implements DeliveryChargesService {

    private final DeliveryChargesRepository deliveryChargesRepository;

    @Override
    public DeliveryChargeResponse create(DeliveryChargeRequest request) {
        validate(request, null);
        DeliveryCharges entity = DeliveryCharges.builder()
                .weightSlab(request.getWeightSlab().trim())
                .weightMin(request.getWeightMin())
                .weightMax(request.getWeightMax())
                .intraCityCharge(request.getIntraCityCharge())
                .metroMetroCharge(request.getMetroMetroCharge())
                .isCustom(request.getIsCustom() != null ? request.getIsCustom() : Boolean.FALSE)
                .status(request.getStatus() != null ? request.getStatus() : Boolean.TRUE)
                .build();

        return toResponse(deliveryChargesRepository.save(entity));
    }

    @Override
    public List<DeliveryChargeResponse> getAll(Boolean status) {
        return deliveryChargesRepository.findWithStatus(status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public DeliveryChargeResponse update(Integer id, DeliveryChargeRequest request) {
        validate(request, id);
        DeliveryCharges entity = deliveryChargesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery charge slab not found"));

        entity.setWeightSlab(request.getWeightSlab().trim());
        entity.setWeightMin(request.getWeightMin());
        entity.setWeightMax(request.getWeightMax());
        entity.setIntraCityCharge(request.getIntraCityCharge());
        entity.setMetroMetroCharge(request.getMetroMetroCharge());
        entity.setIsCustom(request.getIsCustom() != null ? request.getIsCustom() : entity.getIsCustom());
        entity.setStatus(request.getStatus() != null ? request.getStatus() : entity.getStatus());

        return toResponse(deliveryChargesRepository.save(entity));
    }

    @Override
    public DeliveryChargeResponse updateStatus(Integer id, Boolean status) {
        DeliveryCharges entity = deliveryChargesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery charge slab not found"));
        entity.setStatus(status);
        return toResponse(deliveryChargesRepository.save(entity));
    }

    @Override
    public DeliveryChargeResponse getByWeight(BigDecimal weight) {
        if (weight == null || weight.compareTo(BigDecimal.ZERO) < 0) {
            throw new OrderException("Valid weight is required");
        }
        List<DeliveryCharges> matchingSlabs = deliveryChargesRepository.findActiveByWeight(weight);
        if (matchingSlabs.isEmpty()) {
            throw new ResourceNotFoundException("No delivery slab found for weight " + weight);
        }
        if (matchingSlabs.size() > 1) {
            String slabIds = matchingSlabs.stream()
                    .map(DeliveryCharges::getId)
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new OrderException("Multiple active delivery slabs found for weight "
                    + weight + ". Overlapping slab IDs: " + slabIds);
        }
        DeliveryCharges entity = matchingSlabs.get(0);
        return toResponse(entity);
    }

    private void validate(DeliveryChargeRequest request, Integer existingId) {
        if (request.getWeightMin().compareTo(request.getWeightMax()) > 0) {
            throw new OrderException("weightMin cannot be greater than weightMax");
        }
        Integer excludeId = existingId != null ? existingId : -1;
        List<DeliveryCharges> overlaps = deliveryChargesRepository.findOverlappingSlabs(
                request.getWeightMin(),
                request.getWeightMax(),
                excludeId
        );
        if (!overlaps.isEmpty()) {
            String overlapIds = overlaps.stream()
                    .map(DeliveryCharges::getId)
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new OrderException("Weight range overlaps with existing slab(s): " + overlapIds);
        }
    }

    private DeliveryChargeResponse toResponse(DeliveryCharges entity) {
        return DeliveryChargeResponse.builder()
                .id(entity.getId())
                .weightSlab(entity.getWeightSlab())
                .weightMin(entity.getWeightMin())
                .weightMax(entity.getWeightMax())
                .intraCityCharge(entity.getIntraCityCharge())
                .metroMetroCharge(entity.getMetroMetroCharge())
                .isCustom(entity.getIsCustom())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
