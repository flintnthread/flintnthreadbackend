package com.ecommerce.sellerbackend.service.impl;

import com.ecommerce.sellerbackend.dto.CategoryRequestPayload;
import com.ecommerce.sellerbackend.dto.CategoryRequestResponse;
import com.ecommerce.sellerbackend.entity.CategoryRequest;
import com.ecommerce.sellerbackend.entity.CategoryRequestStatus;
import com.ecommerce.sellerbackend.exception.DuplicateResourceException;
import com.ecommerce.sellerbackend.exception.ResourceNotFoundException;
import com.ecommerce.sellerbackend.repository.CategoryRequestRepository;
import com.ecommerce.sellerbackend.service.CategoryRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryRequestServiceImpl implements CategoryRequestService {

    private final CategoryRequestRepository categoryRequestRepository;

    @Override
    public List<CategoryRequestResponse> listForSeller(Long sellerId) {
        return categoryRequestRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .map(CategoryRequestResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public CategoryRequestResponse create(Long sellerId, CategoryRequestPayload payload) {
        String name = payload.getCategoryName().trim();
        if (categoryRequestRepository.existsBySellerIdAndCategoryNameIgnoreCaseAndStatus(
                sellerId, name, CategoryRequestStatus.pending)) {
            throw new DuplicateResourceException(
                    "You already have a pending request for \"" + name + "\".");
        }

        CategoryRequest request = new CategoryRequest();
        request.setSellerId(sellerId);
        request.setCategoryName(name);
        request.setDescription(requireText(payload.getDescription(), "Description is required"));
        request.setReason(requireText(payload.getReason(), "Reason for request is required"));
        request.setStatus(CategoryRequestStatus.pending);

        return CategoryRequestResponse.from(categoryRequestRepository.save(request));
    }

    @Override
    @Transactional
    public CategoryRequestResponse update(Long sellerId, Long requestId, CategoryRequestPayload payload) {
        CategoryRequest request = categoryRequestRepository.findByIdAndSellerId(requestId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Category request not found."));
        if (request.getStatus() != CategoryRequestStatus.pending) {
            throw new IllegalArgumentException("Only pending requests can be edited.");
        }

        String name = payload.getCategoryName().trim();
        request.setCategoryName(name);
        request.setDescription(requireText(payload.getDescription(), "Description is required"));
        request.setReason(requireText(payload.getReason(), "Reason for request is required"));
        return CategoryRequestResponse.from(categoryRequestRepository.save(request));
    }

    @Override
    @Transactional
    public void delete(Long sellerId, Long requestId) {
        CategoryRequest request = categoryRequestRepository.findByIdAndSellerId(requestId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Category request not found."));
        if (request.getStatus() != CategoryRequestStatus.pending) {
            throw new IllegalArgumentException("Only pending requests can be deleted.");
        }
        categoryRequestRepository.delete(request);
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

