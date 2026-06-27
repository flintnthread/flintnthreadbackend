package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.CategoryRequest;
import com.ecommerce.authdemo.dto.CategoryWithSubDTO;
import com.ecommerce.authdemo.dto.SubCategoryResponseDTO;
import com.ecommerce.authdemo.entity.Category;
import com.ecommerce.authdemo.dto.CategoryTreeDTO;
import com.ecommerce.authdemo.entity.SubCategory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface CategoryService {

    // Get all root categories
    List<Category> getMainCategories();

    // Get sub categories using parentId
    List<Category> getSubCategories(Long parentId);

    // Get category by id
    Category getCategory(Long id);

    // Category tree structure
    List<CategoryTreeDTO> getCategoryTree();

    // Search categories
    List<Category> searchCategories(String keyword);

    // Get subcategories from SubCategory table
    List<CategoryWithSubDTO> getSubCategoriesFromTable(Long categoryId);

    // Search both categories and products
    Map<String, Object> searchAll(String keyword);

    Category uploadCategoryImages(Long categoryId,
                                  MultipartFile bannerImage,
                                  MultipartFile mobileImage);

    SubCategory uploadSubCategoryImages(Long id,
                                        MultipartFile image,
                                        MultipartFile mobileImage);
}


