package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.entity.Faq;
import com.ecommerce.adminbackend.entity.FaqCategory;

import java.util.List;
import java.util.Map;

public interface FaqAdminService {

    List<Map<String, Object>> listCategories();

    Map<String, Object> createCategory(FaqCategory input);

    Map<String, Object> updateCategory(Integer id, FaqCategory input);

    void deleteCategory(Integer id);

    List<Map<String, Object>> listFaqs(Integer categoryId);

    Map<String, Object> createFaq(Integer categoryId, Faq input);

    Map<String, Object> updateFaq(Integer categoryId, Integer faqId, Faq input);

    void deleteFaq(Integer categoryId, Integer faqId);
}
