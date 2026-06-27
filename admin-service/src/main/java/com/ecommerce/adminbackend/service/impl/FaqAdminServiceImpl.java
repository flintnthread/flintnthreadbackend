package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.Faq;
import com.ecommerce.adminbackend.entity.FaqCategory;
import com.ecommerce.adminbackend.exception.ResourceNotFoundException;
import com.ecommerce.adminbackend.repository.FaqCategoryRepository;
import com.ecommerce.adminbackend.repository.FaqRepository;
import com.ecommerce.adminbackend.service.FaqAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FaqAdminServiceImpl extends BaseAdminService implements FaqAdminService {

    private final FaqCategoryRepository categoryRepository;
    private final FaqRepository faqRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCategories() {
        return categoryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(this::toCategory)
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> createCategory(FaqCategory input) {
        requireNonBlank(input.getCategoryName(), "Category name");
        return toCategory(categoryRepository.save(input));
    }

    @Override
    @Transactional
    public Map<String, Object> updateCategory(Integer id, FaqCategory input) {
        FaqCategory category = requireCategory(id);
        if (input.getCategoryName() != null) {
            category.setCategoryName(input.getCategoryName());
        }
        if (input.getCategoryIcon() != null) {
            category.setCategoryIcon(input.getCategoryIcon());
        }
        if (input.getSortOrder() != null) {
            category.setSortOrder(input.getSortOrder());
        }
        if (input.getStatus() != null) {
            category.setStatus(input.getStatus());
        }
        return toCategory(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Integer id) {
        requireCategory(id);
        faqRepository.deleteByCategoryId(id);
        categoryRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listFaqs(Integer categoryId) {
        requireCategory(categoryId);
        return faqRepository.findByCategoryIdOrderBySortOrderAscIdAsc(categoryId).stream()
                .map(this::toFaq)
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> createFaq(Integer categoryId, Faq input) {
        requireCategory(categoryId);
        input.setCategoryId(categoryId);
        requireNonBlank(input.getQuestion(), "Question");
        requireNonBlank(input.getAnswer(), "Answer");
        if (input.getIsSeller() == null) {
            input.setIsSeller(false);
        }
        return toFaq(faqRepository.save(input));
    }

    @Override
    @Transactional
    public Map<String, Object> updateFaq(Integer categoryId, Integer faqId, Faq input) {
        Faq faq = requireFaq(faqId, categoryId);
        if (input.getQuestion() != null) {
            faq.setQuestion(input.getQuestion());
        }
        if (input.getAnswer() != null) {
            faq.setAnswer(input.getAnswer());
        }
        if (input.getSortOrder() != null) {
            faq.setSortOrder(input.getSortOrder());
        }
        if (input.getStatus() != null) {
            faq.setStatus(input.getStatus());
        }
        if (input.getIsSeller() != null) {
            faq.setIsSeller(input.getIsSeller());
        }
        return toFaq(faqRepository.save(faq));
    }

    @Override
    @Transactional
    public void deleteFaq(Integer categoryId, Integer faqId) {
        requireFaq(faqId, categoryId);
        faqRepository.deleteById(faqId);
    }

    private FaqCategory requireCategory(Integer id) {
        return requireFound(categoryRepository.findById(id), "FAQ category not found.");
    }

    private Faq requireFaq(Integer faqId, Integer categoryId) {
        Faq faq = requireFound(faqRepository.findById(faqId), "FAQ not found.");
        if (!faq.getCategoryId().equals(categoryId)) {
            throw new ResourceNotFoundException("FAQ not found in this category.");
        }
        return faq;
    }

    private Map<String, Object> toCategory(FaqCategory category) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", category.getId());
        row.put("categoryName", category.getCategoryName());
        row.put("categoryIcon", category.getCategoryIcon());
        row.put("sortOrder", category.getSortOrder());
        row.put("status", category.getStatus());
        row.put("faqCount", faqRepository.countByCategoryId(category.getId()));
        row.put("createdAt", category.getCreatedAt());
        row.put("updatedAt", category.getUpdatedAt());
        return row;
    }

    private Map<String, Object> toFaq(Faq faq) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", faq.getId());
        row.put("categoryId", faq.getCategoryId());
        row.put("question", faq.getQuestion());
        row.put("answer", faq.getAnswer());
        row.put("sortOrder", faq.getSortOrder());
        row.put("status", faq.getStatus());
        row.put("isSeller", faq.getIsSeller());
        row.put("createdAt", faq.getCreatedAt());
        row.put("updatedAt", faq.getUpdatedAt());
        return row;
    }
}
