package com.ecommerce.adminbackend.controller;

import com.ecommerce.adminbackend.entity.Category;
import com.ecommerce.adminbackend.logging.LogFactory;
import com.ecommerce.adminbackend.service.CategoryAdminService;
import com.ecommerce.adminbackend.util.MediaUrlHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
                toStringValue(request.get("categoryName")),
                toStringValue(request.get("hsnCode")),
                toBigDecimal(request.get("gstPercentage")),
                toStringValue(request.get("categoryImage")),
                toStringValue(request.get("mobileImage")),
                toStringValue(request.get("bannerImage")),
                toBoolean(request.get("status"), true)
        );
        return toCategoryMap(category);
    }

    @PostMapping("/subcategories")
    public Map<String, Object> createSubcategory(@RequestBody Map<String, Object> request) {
        Category category = categoryAdminService.createSubcategory(
                toInteger(request.get("parentId")),
                toStringValue(request.get("categoryName")),
                toStringValue(request.get("hsnCode")),
                toBigDecimal(request.get("gstPercentage")),
                toStringValue(request.get("categoryImage")),
                toStringValue(request.get("mobileImage")),
                toStringValue(request.get("bannerImage")),
                toBoolean(request.get("status"), true)
        );
        return toCategoryMap(category);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateCategory(@PathVariable Integer id, @RequestBody Map<String, Object> request) {
        categoryAdminService.updateCategory(
                id,
                toStringValue(request.get("categoryName")),
                toStringValue(request.get("hsnCode")),
                toBigDecimal(request.get("gstPercentage")),
                toStringValue(request.get("categoryImage")),
                toStringValue(request.get("mobileImage")),
                toStringValue(request.get("bannerImage")),
                request.containsKey("status") ? toBoolean(request.get("status"), true) : null
        );
        return toCategoryMap(categoryAdminService.getCategory(id));
    }

    @PostMapping(value = "/{id}/upload-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadImages(
            @PathVariable Integer id,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "mobileImage", required = false) MultipartFile mobileImage,
            @RequestParam(value = "bannerImage", required = false) MultipartFile bannerImage) {
        return toCategoryMap(categoryAdminService.uploadImages(id, image, mobileImage, bannerImage));
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
        String text = value.toString().trim().replace("%", "");
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
