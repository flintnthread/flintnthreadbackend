package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.CookiesPolicyRequest;
import com.ecommerce.authdemo.dto.CookiesPolicyResponse;
import com.ecommerce.authdemo.entity.CookiesPolicy;
import com.ecommerce.authdemo.repository.CookiesPolicyRepository;
import com.ecommerce.authdemo.service.CookiesPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CookiesPolicyServiceImpl implements CookiesPolicyService {

    private final CookiesPolicyRepository cookiesPolicyRepository;

    @Override
    public CookiesPolicyResponse getLatest() {
        return cookiesPolicyRepository.findTopByOrderByIdDesc()
                .map(this::toResponse)
                .orElseGet(() -> CookiesPolicyResponse.builder()
                        .content("")
                        .build());
    }

    @Override
    public CookiesPolicyResponse upsert(CookiesPolicyRequest request) {
        CookiesPolicy entity = cookiesPolicyRepository.findTopByOrderByIdDesc()
                .orElseGet(CookiesPolicy::new);

        entity.setContent(request.getContent().trim());

        CookiesPolicy saved = cookiesPolicyRepository.save(entity);
        return toResponse(saved);
    }

    private CookiesPolicyResponse toResponse(CookiesPolicy entity) {
        return CookiesPolicyResponse.builder()
                .id(entity.getId())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
