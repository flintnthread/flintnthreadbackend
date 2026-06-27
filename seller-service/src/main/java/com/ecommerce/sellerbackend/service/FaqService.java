package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.support.FaqCategoryResponse;
import com.ecommerce.sellerbackend.dto.support.FaqResponse;
import com.ecommerce.sellerbackend.entity.Faq;
import com.ecommerce.sellerbackend.entity.FaqCategory;
import com.ecommerce.sellerbackend.repository.FaqCategoryRepository;
import com.ecommerce.sellerbackend.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;
    private final FaqCategoryRepository faqCategoryRepository;

    public List<FaqResponse> getAllFaqs(boolean sellerOnly) {
        Map<Integer, String> categoryNames = loadCategoryNames();
        List<Faq> faqs = sellerOnly
                ? faqRepository.findActiveSellerFaqs()
                : faqRepository.findAllActiveFaqs();
        return faqs.stream()
                .map(f -> toResponse(f, categoryNames.get(f.getCategoryId())))
                .toList();
    }

    public List<FaqResponse> searchFaqs(String query, boolean sellerOnly) {
        if (query == null || query.isBlank()) {
            return getAllFaqs(sellerOnly);
        }
        Map<Integer, String> categoryNames = loadCategoryNames();
        List<Faq> faqs = sellerOnly
                ? faqRepository.searchActiveSellerFaqs(query.trim())
                : faqRepository.searchAllActiveFaqs(query.trim());
        return faqs.stream()
                .map(f -> toResponse(f, categoryNames.get(f.getCategoryId())))
                .toList();
    }

    private Map<Integer, String> loadCategoryNames() {
        return faqCategoryRepository.findByStatusTrueOrderBySortOrderAscIdAsc().stream()
                .collect(Collectors.toMap(FaqCategory::getId, FaqCategory::getCategoryName));
    }

    public List<FaqCategoryResponse> getGroupedFaqs(boolean sellerOnly) {
        List<FaqCategory> categories = faqCategoryRepository.findByStatusTrueOrderBySortOrderAscIdAsc();
        List<Faq> faqs = sellerOnly
                ? faqRepository.findActiveSellerFaqs()
                : faqRepository.findAllActiveFaqs();

        Map<Integer, String> categoryNames = categories.stream()
                .collect(Collectors.toMap(FaqCategory::getId, FaqCategory::getCategoryName));

        Map<Integer, List<FaqResponse>> faqsByCategory = faqs.stream()
                .map(f -> toResponse(f, categoryNames.get(f.getCategoryId())))
                .collect(Collectors.groupingBy(
                        FaqResponse::getCategoryId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return categories.stream()
                .map(cat -> FaqCategoryResponse.builder()
                        .id(cat.getId())
                        .categoryName(cat.getCategoryName())
                        .categoryIcon(cat.getCategoryIcon())
                        .sortOrder(cat.getSortOrder())
                        .faqs(faqsByCategory.getOrDefault(cat.getId(), List.of()))
                        .build())
                .filter(cat -> !cat.getFaqs().isEmpty())
                .toList();
    }

    private FaqResponse toResponse(Faq faq) {
        return toResponse(faq, null);
    }

    private FaqResponse toResponse(Faq faq, String categoryName) {
        return FaqResponse.builder()
                .id(faq.getId())
                .categoryId(faq.getCategoryId())
                .categoryName(categoryName)
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .sortOrder(faq.getSortOrder())
                .isSeller(faq.getIsSeller())
                .build();
    }
}
