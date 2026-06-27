package com.ecommerce.sellerbackend.controller;

import com.ecommerce.sellerbackend.dto.support.FaqCategoryResponse;
import com.ecommerce.sellerbackend.dto.support.FaqResponse;
import com.ecommerce.sellerbackend.service.FaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/support/faqs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FaqController {

    private final FaqService faqService;

    /** All active FAQs (flat list for search / general section) */
    @GetMapping
    public ResponseEntity<List<FaqResponse>> getFaqs(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "true") boolean sellerOnly) {
        if (q != null && !q.isBlank()) {
            return ResponseEntity.ok(faqService.searchFaqs(q, sellerOnly));
        }
        return ResponseEntity.ok(faqService.getAllFaqs(sellerOnly));
    }

    /** FAQs grouped by category (for Help Topics section) */
    @GetMapping("/grouped")
    public ResponseEntity<List<FaqCategoryResponse>> getGroupedFaqs(
            @RequestParam(defaultValue = "true") boolean sellerOnly) {
        return ResponseEntity.ok(faqService.getGroupedFaqs(sellerOnly));
    }
}
