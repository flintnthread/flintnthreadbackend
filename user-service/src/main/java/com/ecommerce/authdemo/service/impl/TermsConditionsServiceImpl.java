package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.TermsConditionsRequest;
import com.ecommerce.authdemo.dto.TermsConditionsResponse;
import com.ecommerce.authdemo.entity.TermsConditions;
import com.ecommerce.authdemo.repository.TermsConditionsRepository;
import com.ecommerce.authdemo.service.TermsConditionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TermsConditionsServiceImpl implements TermsConditionsService {

    private final TermsConditionsRepository termsConditionsRepository;

    @Override
    public TermsConditionsResponse getLatest() {
        return termsConditionsRepository.findTopByOrderByIdDesc()
                .map(this::toResponse)
                .orElseGet(() -> TermsConditionsResponse.builder()
                        .content("")
                        .build());
    }

    @Override
    public TermsConditionsResponse upsert(TermsConditionsRequest request) {
        TermsConditions entity = termsConditionsRepository.findTopByOrderByIdDesc()
                .orElseGet(TermsConditions::new);
        entity.setContent(request.getContent());
        return toResponse(termsConditionsRepository.save(entity));
    }

    private TermsConditionsResponse toResponse(TermsConditions entity) {
        return TermsConditionsResponse.builder()
                .id(entity.getId())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
