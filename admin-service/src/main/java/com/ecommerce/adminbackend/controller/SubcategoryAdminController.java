package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.entity.Category;
import com.ecommerce.adminbackend.entity.Subcategory;
import com.ecommerce.adminbackend.repository.CategoryRepository;
import com.ecommerce.adminbackend.service.SubcategoryAdminService;
import com.ecommerce.adminbackend.util.MediaUrlHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/{id}")
    public Map<String, Object> getSubcategory(@PathVariable Integer id) {
        return toSubcategoryMap(subcategoryAdminService.getSubcategory(id));
    }

    @PostMapping
    public Map<String, Object> createSubcategory(@RequestBody Map<String, Object> request) {
        Subcategory subcategory = subcategoryAdminService.createSubcategory(
                toInteger(request.get("categoryId")),
                toStringValue(request.get("subcategoryName")),
                toStringValue(request.get("subcategoryImage")),
                toStringValue(request.get("mobileImage")),
                toStringValue(request.get("materialSlabs")),
                toStringValue(request.get("weightSlabs")),
                toBigDecimal(request.get("gstPercentage")),
                toBoolean(request.get("status"), true)
        );
        return toSubcategoryMap(subcategory);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateSubcategory(@PathVariable Integer id, @RequestBody Map<String, Object> request) {
        Subcategory subcategory = subcategoryAdminService.updateSubcategory(
                id,
                toInteger(request.get("categoryId")),
                toStringValue(request.get("subcategoryName")),
                toStringValue(request.get("subcategoryImage")),
                toStringValue(request.get("mobileImage")),
                toStringValue(request.get("materialSlabs")),
                toStringValue(request.get("weightSlabs")),
                toBigDecimal(request.get("gstPercentage")),
                request.containsKey("status") ? toBoolean(request.get("status"), true) : null
        );
        return toSubcategoryMap(subcategory);
    }

    @PostMapping(value = "/{id}/upload-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadImages(
            @PathVariable Integer id,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "mobileImage", required = false) MultipartFile mobileImage) {
        return toSubcategoryMap(subcategoryAdminService.uploadImages(id, image, mobileImage));
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

        Optional<Category> categoryOpt = categoryRepository.findById(subcategory.getCategoryId());
        if (categoryOpt.isPresent()) {
            Category category = categoryOpt.get();
            map.put("category", category.getCategoryName());
            map.put("categoryImage", mediaUrlHelper.toPublicUrl(category.getCategoryImage(), "categories"));
            map.put("mobileCategoryImage", mediaUrlHelper.toPublicUrl(category.getMobileImage(), "categories"));

            if (category.getParentId() != null) {
                Optional<Category> mainCategoryOpt = categoryRepository.findById(category.getParentId());
                if (mainCategoryOpt.isPresent()) {
                    map.put("mainCat", mainCategoryOpt.get().getCategoryName());
                }
            } else {
                map.put("mainCat", category.getCategoryName());
            }
        }

        return map;
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : Integer.valueOf(text);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : new BigDecimal(text);
    }

    private static Boolean toBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.valueOf(value.toString());
    }

    private static String toStringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
