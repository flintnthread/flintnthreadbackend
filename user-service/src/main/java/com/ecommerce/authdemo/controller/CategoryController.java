package com.ecommerce.authdemo.controller;

import com.ecommerce.authdemo.dto.CategoryRequest;
import com.ecommerce.authdemo.dto.CategoryWithSubDTO;
import com.ecommerce.authdemo.dto.SubCategoryResponseDTO;
import com.ecommerce.authdemo.entity.Category;
import com.ecommerce.authdemo.dto.CategoryTreeDTO;
import com.ecommerce.authdemo.entity.SubCategory;
import com.ecommerce.authdemo.service.CategoryService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 1️⃣ Get all MAIN categories
     */
    @GetMapping("/main")
    public ResponseEntity<List<Category>> getMainCategories() {

        List<Category> categories = categoryService.getMainCategories();
        return ResponseEntity.ok(categories);
    }


    /**
     * 2️⃣ Get subcategories from CATEGORY TABLE
     */
    @GetMapping("/{parentId}/subcategories")
    public ResponseEntity<List<Category>> getSubCategories(
            @PathVariable Long parentId) {

        List<Category> categories = categoryService.getSubCategories(parentId);
        return ResponseEntity.ok(categories);
    }


    /**
     * 3️⃣ Get category by ID
     */
    @GetMapping("/id/{id}")
    public ResponseEntity<Category> getCategoryById(
            @PathVariable Long id) {

        Category category = categoryService.getCategory(id);
        return ResponseEntity.ok(category);
    }


    /**
     * 4️⃣ Get FULL CATEGORY TREE
     * Used for nested category UI
     */
    @GetMapping("/tree")
    public ResponseEntity<List<CategoryTreeDTO>> getCategoryTree() {

        List<CategoryTreeDTO> tree = categoryService.getCategoryTree();
        return ResponseEntity.ok(tree);
    }


    /**
     * 5️⃣ Search only categories
     */
    @GetMapping("/search")
    public ResponseEntity<List<Category>> searchCategories(
            @RequestParam String keyword) {

        List<Category> categories = categoryService.searchCategories(keyword);
        return ResponseEntity.ok(categories);
    }


    /**
     * 6️⃣ Get SubCategories from SUBCATEGORY TABLE
     */
    @GetMapping("/{categoryId}/subcategories-table")
    public ResponseEntity<List<CategoryWithSubDTO>> getSubCategoriesFromTable(
            @PathVariable Long categoryId) {

        return ResponseEntity.ok(
                categoryService.getSubCategoriesFromTable(categoryId)
        );
    }


    /**
     * 7️⃣ HOME SEARCH (Category + Product)
     */
    @GetMapping("/search-all")
    public ResponseEntity<Map<String, Object>> searchAll(
            @RequestParam String keyword) {

        Map<String, Object> result = categoryService.searchAll(keyword);
        return ResponseEntity.ok(result);
    }

    /**
     * Upload main category images to Cloudinary.
     * POST /api/categories/{id}/upload-images
     * form-data: bannerImage (file, optional), mobileImage (file, optional)
     */
    @PostMapping("/{id}/upload-images")
    public ResponseEntity<Category> uploadCategoryImages(
            @PathVariable Long id,
            @RequestParam(value = "bannerImage", required = false) MultipartFile bannerImage,
            @RequestParam(value = "mobileImage", required = false) MultipartFile mobileImage) {

        if ((bannerImage == null || bannerImage.isEmpty())
                && (mobileImage == null || mobileImage.isEmpty())) {
            return ResponseEntity.badRequest().build();
        }

        Category updated = categoryService.uploadCategoryImages(id, bannerImage, mobileImage);
        return ResponseEntity.ok(updated);
    }

    /**
     * Upload subcategory images to Cloudinary.
     * POST /api/categories/subcategories/{id}/upload-images
     * form-data: image (file, optional), mobileImage (file, optional)
     */
    @PostMapping("/subcategories/{id}/upload-images")
    public ResponseEntity<SubCategory> uploadSubCategoryImages(
            @PathVariable Long id,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "mobileImage", required = false) MultipartFile mobileImage) {

        if ((image == null || image.isEmpty()) && (mobileImage == null || mobileImage.isEmpty())) {
            return ResponseEntity.badRequest().build();
        }

        SubCategory updated = categoryService.uploadSubCategoryImages(id, image, mobileImage);
        return ResponseEntity.ok(updated);
    }

}