package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.FaqCategoryRequest;
import com.ecommerce.authdemo.dto.FaqCategoryResponse;
import com.ecommerce.authdemo.entity.FaqCategory;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.FaqCategoryRepository;
import com.ecommerce.authdemo.service.FaqCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqCategoryServiceImpl implements FaqCategoryService {

    private final FaqCategoryRepository faqCategoryRepository;

    @Override
    public FaqCategoryResponse create(FaqCategoryRequest request) {
        FaqCategory entity = FaqCategory.builder()
                .categoryName(request.getCategoryName().trim())
                .categoryIcon(normalize(request.getCategoryIcon()))
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .status(request.getStatus() != null ? request.getStatus() : Boolean.TRUE)
                .build();
        return toResponse(faqCategoryRepository.save(entity));
    }

    @Override
    public List<FaqCategoryResponse> getAll(Boolean status) {
        return faqCategoryRepository.findWithStatus(status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public FaqCategoryResponse update(Integer id, FaqCategoryRequest request) {
        FaqCategory entity = faqCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ category not found"));

        entity.setCategoryName(request.getCategoryName().trim());
        entity.setCategoryIcon(normalize(request.getCategoryIcon()));
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : entity.getSortOrder());
        entity.setStatus(request.getStatus() != null ? request.getStatus() : entity.getStatus());

        return toResponse(faqCategoryRepository.save(entity));
    }

    @Override
    public FaqCategoryResponse updateStatus(Integer id, Boolean status) {
        FaqCategory entity = faqCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ category not found"));
        entity.setStatus(status);
        return toResponse(faqCategoryRepository.save(entity));
    }

    @Override
    public void delete(Integer id) {
        FaqCategory entity = faqCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ category not found"));
        faqCategoryRepository.delete(entity);
    }

    private FaqCategoryResponse toResponse(FaqCategory entity) {
        return FaqCategoryResponse.builder()
                .id(entity.getId())
                .categoryName(entity.getCategoryName())
                .categoryIcon(entity.getCategoryIcon())
                .sortOrder(entity.getSortOrder())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
