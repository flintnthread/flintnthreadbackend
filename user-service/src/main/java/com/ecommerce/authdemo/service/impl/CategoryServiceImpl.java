package com.ecommerce.authdemo.service.impl;

import com.ecommerce.authdemo.dto.CategoryTreeDTO;
import com.ecommerce.authdemo.dto.CategoryWithSubDTO;
import com.ecommerce.authdemo.dto.SubCategoryResponseDTO;
import com.ecommerce.authdemo.entity.Category;
import com.ecommerce.authdemo.entity.Product;
import com.ecommerce.authdemo.entity.SubCategory;
import com.ecommerce.authdemo.util.ProductCatalogVisibility;
import com.ecommerce.authdemo.repository.CategoryRepository;
import com.ecommerce.authdemo.repository.ProductRepository;
import com.ecommerce.authdemo.repository.SubCategoryRepository;
import com.ecommerce.authdemo.service.CategoryService;

import com.ecommerce.authdemo.service.ImageUploadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ProductRepository productRepository;
    private final ImageUploadService imageUploadService;


    // Constructor Injection
    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               SubCategoryRepository subCategoryRepository,
                               ProductRepository productRepository,
                               ImageUploadService imageUploadService)
    {

        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.productRepository = productRepository;
        this.imageUploadService = imageUploadService;
    }

    /**
     * Get all main categories
     */
    @Override
    public List<Category> getMainCategories() {

        return categoryRepository.findByParentIdIsNull()
                .stream()
                .filter(c -> c.getStatus() != null && c.getStatus() == 1)
                .sorted(Comparator.comparing(Category::getCategoryName))
                .collect(Collectors.toList());
    }

    /**
     * Get sub categories
     */
    @Override
    public List<Category> getSubCategories(Long parentId) {

        if (parentId == null) {
            throw new IllegalArgumentException("Parent ID cannot be null");
        }

        return categoryRepository.findByParentId(parentId)
                .stream()
                .filter(c -> c.getStatus() != null && c.getStatus() == 1)
                .sorted(Comparator.comparing(Category::getCategoryName))
                .collect(Collectors.toList());
    }

    /**
     * Get category by id
     */
    @Override
    public Category getCategory(Long id) {

        if (id == null) {
            throw new IllegalArgumentException("Category ID cannot be null");
        }

        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Category not found with id: " + id));
    }

    /**
     * Get full category tree
     */
    @Override
    public List<CategoryTreeDTO> getCategoryTree() {

        List<Category> allCategories = categoryRepository.findAll();

        Map<Long, List<Category>> categoryMap =
                allCategories.stream()
                        .collect(Collectors.groupingBy(
                                c -> c.getParentId() == null ? 0L : c.getParentId()
                        ));

        List<Category> rootCategories = categoryMap.getOrDefault(0L, new ArrayList<>());

        return rootCategories.stream()
                .map(category -> buildTree(category, categoryMap))
                .collect(Collectors.toList());
    }

    /**
     * Recursive method to build category tree
     */
    private CategoryTreeDTO buildTree(Category category,
                                      Map<Long, List<Category>> categoryMap) {

        CategoryTreeDTO dto = new CategoryTreeDTO();

        dto.setId(category.getId());
        dto.setName(category.getCategoryName());
        dto.setImage(category.getImage());

        List<Category> children =
                categoryMap.getOrDefault(category.getId(), new ArrayList<>());

        dto.setChildren(
                children.stream()
                        .map(child -> buildTree(child, categoryMap))
                        .collect(Collectors.toList())
        );

        return dto;
    }

    /**
     * Search categories
     */
    @Override
    public List<Category> searchCategories(String keyword) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return categoryRepository
                .findByCategoryNameContainingIgnoreCase(keyword.trim())
                .stream()
                .filter(c -> c.getStatus() != null && c.getStatus() == 1)
                .collect(Collectors.toList());
    }

    /**
     * Get sub categories from SubCategory table
     */
    @Override
    public List<CategoryWithSubDTO> getSubCategoriesFromTable(Long categoryId) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        List<SubCategory> subCategories =
                subCategoryRepository.findByCategoryId(categoryId);

        List<SubCategoryResponseDTO> subCategoryList =
                subCategories.stream()
                        .map(sc -> new SubCategoryResponseDTO(
                                sc.getId(),
                                sc.getSubcategoryName(),
                                sc.getSubcategoryImage(),
                                sc.getMobileImage()

                        ))
                        .toList();

        CategoryWithSubDTO response = new CategoryWithSubDTO();
        response.setCategoryName(category.getCategoryName());
        response.setSubcategories(subCategoryList);
        response.setMobileImage(category.getMobileImage()); // ✅ add


        return List.of(response);
    }

    /**
     * Search both categories and products
     */
    @Override

    public Map<String, Object> searchAll(String keyword) {

        Map<String, Object> result = new HashMap<>();

        if (keyword == null || keyword.trim().isEmpty()) {
            result.put("categories", Collections.emptyList());
            result.put("products", Collections.emptyList());
            return result;
        }

        List<Category> categories =
                categoryRepository.findByCategoryNameContainingIgnoreCase(keyword);

        List<Product> products =
                productRepository.findTop20ByNameContainingIgnoreCaseAndStatus(
                        keyword, ProductCatalogVisibility.USER_VISIBLE_STATUS);

        result.put("categories", categories);
        result.put("products", products);

        return result;
    }

    @Override
    @Transactional
    public Category uploadCategoryImages(Long categoryId,
                                         MultipartFile bannerImage,
                                         MultipartFile mobileImage) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Banner image optional
        if (bannerImage != null && !bannerImage.isEmpty()) {
            String bannerUrl = imageUploadService.uploadImage(bannerImage, "categories/banner");
            category.setBannerImage(bannerUrl);
        }

        // Mobile image optional
        if (mobileImage != null && !mobileImage.isEmpty()) {
            String mobileUrl = imageUploadService.uploadImage(mobileImage, "categories/mobile");
            category.setMobileImage(mobileUrl);
        }

        return categoryRepository.save(category);
    }

    @Transactional
    @Override
    public SubCategory uploadSubCategoryImages(Long id,
                                               MultipartFile image,
                                               MultipartFile mobileImage) {

        SubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubCategory not found"));

        // Subcategory image optional
        if (image != null && !image.isEmpty()) {
            String imageUrl = imageUploadService.uploadImage(image, "subcategories/image");
            subCategory.setSubcategoryImage(imageUrl);
        }

        // Mobile image optional
        if (mobileImage != null && !mobileImage.isEmpty()) {
            String mobileUrl = imageUploadService.uploadImage(mobileImage, "subcategories/mobile");
            subCategory.setMobileImage(mobileUrl);
        }

        return subCategoryRepository.save(subCategory);
    }

}