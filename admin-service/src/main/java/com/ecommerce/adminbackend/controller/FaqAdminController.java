package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.logging.LogFactory;
import org.slf4j.Logger;
import com.ecommerce.adminbackend.entity.Faq;
import com.ecommerce.adminbackend.entity.FaqCategory;
import com.ecommerce.adminbackend.service.FaqAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/faq")
@RequiredArgsConstructor
public class FaqAdminController {

    private static final Logger log = LogFactory.getLogger(FaqAdminController.class);

    private final FaqAdminService faqAdminService;

    @GetMapping("/categories")
    public List<Map<String, Object>> listCategories() {
        return faqAdminService.listCategories();
    }

    @PostMapping("/categories")
    public Map<String, Object> createCategory(@RequestBody FaqCategory request) {
        return faqAdminService.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    public Map<String, Object> updateCategory(@PathVariable Integer id, @RequestBody FaqCategory request) {
        return faqAdminService.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    public void deleteCategory(@PathVariable Integer id) {
        faqAdminService.deleteCategory(id);
    }

    @GetMapping("/categories/{id}/faqs")
    public List<Map<String, Object>> listFaqs(@PathVariable Integer id) {
        return faqAdminService.listFaqs(id);
    }

    @PostMapping("/categories/{id}/faqs")
    public Map<String, Object> createFaq(@PathVariable Integer id, @RequestBody Faq request) {
        return faqAdminService.createFaq(id, request);
    }

    @PutMapping("/categories/{categoryId}/faqs/{faqId}")
    public Map<String, Object> updateFaq(
            @PathVariable Integer categoryId,
            @PathVariable Integer faqId,
            @RequestBody Faq request) {
        return faqAdminService.updateFaq(categoryId, faqId, request);
    }

    @DeleteMapping("/categories/{categoryId}/faqs/{faqId}")
    public void deleteFaq(@PathVariable Integer categoryId, @PathVariable Integer faqId) {
        faqAdminService.deleteFaq(categoryId, faqId);
    }
}
