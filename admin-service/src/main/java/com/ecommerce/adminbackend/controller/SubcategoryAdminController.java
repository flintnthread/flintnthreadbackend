package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.entity.Category;
import com.ecommerce.adminbackend.entity.Subcategory;
import com.ecommerce.adminbackend.repository.CategoryRepository;
import com.ecommerce.adminbackend.service.SubcategoryAdminService;
import com.ecommerce.adminbackend.util.MediaUrlHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/subcategories")
@RequiredArgsConstructor
public class SubcategoryAdminController {

    private final SubcategoryAdminService subcategoryAdminService;
    private final MediaUrlHelper mediaUrlHelper;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public List<Map<String, Object>> listSubcategories(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String search) {
        List<Subcategory> subcategories = subcategoryAdminService.listSubcategories(categoryId, search);
        return subcategories.stream()
                .map(this::toSubcategoryMap)
                .toList();
    }

    @GetMapping("/counts")
    public Map<String, Long> getCounts() {
        return subcategoryAdminService.getCounts();
    }

    @PostMapping
    public Map<String, Object> createSubcategory(@RequestBody Map<String, Object> request) {
        Subcategory subcategory = subcategoryAdminService.createSubcategory(
                (Integer) request.get("categoryId"),
                (String) request.get("subcategoryName"),
                (String) request.get("subcategoryImage"),
                (String) request.get("mobileImage"),
                (String) request.get("materialSlabs"),
                (String) request.get("weightSlabs"),
                request.get("gstPercentage") != null ? new BigDecimal(request.get("gstPercentage").toString()) : null,
                request.get("status") != null ? Boolean.valueOf(request.get("status").toString()) : true
        );
        return toSubcategoryMap(subcategory);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateSubcategory(@PathVariable Integer id, @RequestBody Map<String, Object> request) {
        subcategoryAdminService.updateSubcategory(
                id,
                (Integer) request.get("categoryId"),
                (String) request.get("subcategoryName"),
                (String) request.get("subcategoryImage"),
                (String) request.get("mobileImage"),
                (String) request.get("materialSlabs"),
                (String) request.get("weightSlabs"),
                request.get("gstPercentage") != null ? new BigDecimal(request.get("gstPercentage").toString()) : null,
                request.get("status") != null ? Boolean.valueOf(request.get("status").toString()) : true
        );
        Subcategory subcategory = subcategoryAdminService.listSubcategories(null, null).stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElse(null);
        return subcategory != null ? toSubcategoryMap(subcategory) : Map.of("id", id);
    }

    @DeleteMapping("/{id}")
    public void deleteSubcategory(@PathVariable Integer id) {
        subcategoryAdminService.deleteSubcategory(id);
    }

    private Map<String, Object> toSubcategoryMap(Subcategory subcategory) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", subcategory.getId());
        map.put("categoryId", subcategory.getCategoryId());
        map.put("subcategoryName", subcategory.getSubcategoryName());
        map.put("subcategoryImage", mediaUrlHelper.toPublicUrl(subcategory.getSubcategoryImage(), "subcategories"));
        map.put("mobileImage", mediaUrlHelper.toPublicUrl(subcategory.getMobileImage(), "subcategories"));
        map.put("materialSlabs", subcategory.getMaterialSlabs());
        map.put("weightSlabs", subcategory.getWeightSlabs());
        map.put("gstPercentage", subcategory.getGstPercentage());
        map.put("status", subcategory.getStatus());
        map.put("createdAt", subcategory.getCreatedAt());
        map.put("sellerId", subcategory.getSellerId());

        // Add category information
        Optional<Category> categoryOpt = categoryRepository.findById(subcategory.getCategoryId());
        if (categoryOpt.isPresent()) {
            Category category = categoryOpt.get();
            map.put("category", category.getCategoryName());
            map.put("categoryImage", mediaUrlHelper.toPublicUrl(category.getCategoryImage(), "categories"));
            map.put("mobileCategoryImage", mediaUrlHelper.toPublicUrl(category.getMobileImage(), "categories"));

            // Add main category information if this category has a parent
            if (category.getParentId() != null) {
                Optional<Category> mainCategoryOpt = categoryRepository.findById(category.getParentId());
                if (mainCategoryOpt.isPresent()) {
                    map.put("mainCat", mainCategoryOpt.get().getCategoryName());
                }
            } else {
                // If the category itself is a main category (no parent)
                map.put("mainCat", category.getCategoryName());
            }
        }

        return map;
    }
}
