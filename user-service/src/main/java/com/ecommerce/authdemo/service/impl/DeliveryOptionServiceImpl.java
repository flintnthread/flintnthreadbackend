package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.DeliveryOptionRequest;
import com.ecommerce.authdemo.dto.DeliveryOptionResponse;
import com.ecommerce.authdemo.entity.DeliveryOption;
import com.ecommerce.authdemo.exception.OrderException;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.DeliveryOptionRepository;
import com.ecommerce.authdemo.service.DeliveryOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryOptionServiceImpl implements DeliveryOptionService {

    private final DeliveryOptionRepository deliveryOptionRepository;

    @Override
    public DeliveryOptionResponse create(DeliveryOptionRequest request) {
        validate(request);
        DeliveryOption entity = DeliveryOption.builder()
                .sellerId(request.getSellerId())
                .optionName(request.getOptionName().trim())
                .minDays(request.getMinDays())
                .maxDays(request.getMaxDays())
                .deliveryInfo(normalize(request.getDeliveryInfo()))
                .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
                .build();
        return toResponse(deliveryOptionRepository.save(entity));
    }

    @Override
    public List<DeliveryOptionResponse> getAll(Integer sellerId, Boolean isActive) {
        return deliveryOptionRepository.findWithFilters(sellerId, isActive)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public DeliveryOptionResponse update(Integer id, DeliveryOptionRequest request) {
        validate(request);
        DeliveryOption entity = deliveryOptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery option not found"));

        entity.setSellerId(request.getSellerId());
        entity.setOptionName(request.getOptionName().trim());
        entity.setMinDays(request.getMinDays());
        entity.setMaxDays(request.getMaxDays());
        entity.setDeliveryInfo(normalize(request.getDeliveryInfo()));
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : entity.getIsActive());
        return toResponse(deliveryOptionRepository.save(entity));
    }

    @Override
    public DeliveryOptionResponse updateStatus(Integer id, Boolean isActive) {
        DeliveryOption entity = deliveryOptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery option not found"));
        entity.setIsActive(isActive);
        return toResponse(deliveryOptionRepository.save(entity));
    }

    private void validate(DeliveryOptionRequest request) {
        if (request.getMinDays() > request.getMaxDays()) {
            throw new OrderException("minDays cannot be greater than maxDays");
        }
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private DeliveryOptionResponse toResponse(DeliveryOption entity) {
        return DeliveryOptionResponse.builder()
                .id(entity.getId())
                .sellerId(entity.getSellerId())
                .optionName(entity.getOptionName())
                .minDays(entity.getMinDays())
                .maxDays(entity.getMaxDays())
                .deliveryInfo(entity.getDeliveryInfo())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
