package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.FaqRequest;
import com.ecommerce.authdemo.dto.FaqResponse;
import com.ecommerce.authdemo.entity.Faq;
import com.ecommerce.authdemo.exception.ResourceNotFoundException;
import com.ecommerce.authdemo.repository.FaqCategoryRepository;
import com.ecommerce.authdemo.repository.FaqRepository;
import com.ecommerce.authdemo.service.FaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqServiceImpl implements FaqService {

    private final FaqRepository faqRepository;
    private final FaqCategoryRepository faqCategoryRepository;

    @Override
    public FaqResponse create(FaqRequest request) {
        validateCategory(request.getCategoryId());

        Faq entity = Faq.builder()
                .categoryId(request.getCategoryId())
                .question(request.getQuestion().trim())
                .answer(request.getAnswer().trim())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .status(request.getStatus() != null ? request.getStatus() : Boolean.TRUE)
                .build();

        return toResponse(faqRepository.save(entity));
    }

    @Override
    public List<FaqResponse> getAll(Integer categoryId, Boolean status) {
        return faqRepository.findWithFilters(categoryId, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public FaqResponse update(Integer id, FaqRequest request) {
        validateCategory(request.getCategoryId());

        Faq entity = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found"));

        entity.setCategoryId(request.getCategoryId());
        entity.setQuestion(request.getQuestion().trim());
        entity.setAnswer(request.getAnswer().trim());
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : entity.getSortOrder());
        entity.setStatus(request.getStatus() != null ? request.getStatus() : entity.getStatus());

        return toResponse(faqRepository.save(entity));
    }

    @Override
    public FaqResponse updateStatus(Integer id, Boolean status) {
        Faq entity = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found"));
        entity.setStatus(status);
        return toResponse(faqRepository.save(entity));
    }

    @Override
    public void delete(Integer id) {
        Faq entity = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found"));
        faqRepository.delete(entity);
    }

    private void validateCategory(Integer categoryId) {
        if (!faqCategoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("FAQ category not found");
        }
    }

    private FaqResponse toResponse(Faq entity) {
        return FaqResponse.builder()
                .id(entity.getId())
                .categoryId(entity.getCategoryId())
                .question(entity.getQuestion())
                .answer(entity.getAnswer())
                .sortOrder(entity.getSortOrder())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
