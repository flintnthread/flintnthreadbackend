package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.entity.Category;
import com.ecommerce.adminbackend.logging.LogFactory;
import com.ecommerce.adminbackend.service.CategoryAdminService;
import com.ecommerce.adminbackend.util.MediaUrlHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class CategoryAdminController {

    private static final Logger log = LogFactory.getLogger(CategoryAdminController.class);

    private final CategoryAdminService categoryAdminService;
    private final MediaUrlHelper mediaUrlHelper;

    @GetMapping("/main")
    public List<Map<String, Object>> mainCategories(@RequestParam(required = false) String search) {
        List<Category> categories = categoryAdminService.listMainCategories(search);
        return categories.stream().map(this::toCategoryMap).toList();
    }

    @GetMapping("/subcategories")
    public List<Map<String, Object>> subcategories(
            @RequestParam(required = false) Integer parentId,
            @RequestParam(required = false) String search) {
        List<Category> categories = categoryAdminService.listSubcategories(parentId, search);
        return categories.stream().map(this::toCategoryMap).toList();
    }

    @GetMapping("/counts")
    public Map<String, Long> counts() {
        return categoryAdminService.getCounts();
    }

    @PostMapping("/main")
    public Map<String, Object> createMainCategory(@RequestBody Map<String, Object> request) {
        Category category = categoryAdminService.createMainCategory(
                (String) request.get("categoryName"),
                (String) request.get("hsnCode"),
                request.get("gstPercentage") != null ? new BigDecimal(request.get("gstPercentage").toString()) : null,
                (String) request.get("categoryImage"),
                (String) request.get("mobileImage"),
                (String) request.get("bannerImage"),
                request.get("status") != null ? Boolean.valueOf(request.get("status").toString()) : true
        );
        return toCategoryMap(category);
    }

    @PostMapping("/subcategories")
    public Map<String, Object> createSubcategory(@RequestBody Map<String, Object> request) {
        Category category = categoryAdminService.createSubcategory(
                (Integer) request.get("parentId"),
                (String) request.get("categoryName"),
                (String) request.get("hsnCode"),
                request.get("gstPercentage") != null ? new BigDecimal(request.get("gstPercentage").toString()) : null,
                (String) request.get("categoryImage"),
                (String) request.get("mobileImage"),
                (String) request.get("bannerImage"),
                request.get("status") != null ? Boolean.valueOf(request.get("status").toString()) : true
        );
        return toCategoryMap(category);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateCategory(@PathVariable Integer id, @RequestBody Map<String, Object> request) {
        categoryAdminService.updateCategory(
                id,
                (String) request.get("categoryName"),
                (String) request.get("hsnCode"),
                request.get("gstPercentage") != null ? new BigDecimal(request.get("gstPercentage").toString()) : null,
                (String) request.get("categoryImage"),
                (String) request.get("mobileImage"),
                (String) request.get("bannerImage"),
                request.get("status") != null ? Boolean.valueOf(request.get("status").toString()) : true
        );
        Category category = categoryAdminService.listMainCategories(null).stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (category == null) {
            category = categoryAdminService.listSubcategories(null, null).stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }
        return category != null ? toCategoryMap(category) : Map.of("id", id);
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Integer id) {
        categoryAdminService.deleteCategory(id);
    }

    private Map<String, Object> toCategoryMap(Category category) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", category.getId());
        map.put("categoryName", category.getCategoryName());
        map.put("parentId", category.getParentId());
        map.put("categoryImage", mediaUrlHelper.toPublicUrl(category.getCategoryImage(), "categories"));
        map.put("mobileImage", mediaUrlHelper.toPublicUrl(category.getMobileImage(), "categories"));
        map.put("bannerImage", mediaUrlHelper.toPublicUrl(category.getBannerImage(), "categories"));
        map.put("hsnCode", category.getHsnCode());
        map.put("gstPercentage", category.getGstPercentage());
        map.put("status", category.getStatus());
        map.put("createdAt", category.getCreatedAt());
        return map;
    }
}
