package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.Category;
import com.ecommerce.adminbackend.repository.CategoryRepository;
import com.ecommerce.adminbackend.service.CatalogImageStorageService;
import com.ecommerce.adminbackend.service.CategoryAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryAdminServiceImpl extends BaseAdminService implements CategoryAdminService {

    private final CategoryRepository categoryRepository;
    private final CatalogImageStorageService catalogImageStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<Category> listMainCategories(String search) {
        if (search == null || search.trim().isEmpty()) {
            return categoryRepository.findByParentIdIsNullOrderByCategoryNameAsc();
        }
        return categoryRepository.searchByName(search.trim()).stream()
                .filter(c -> c.getParentId() == null)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> listSubcategories(Integer parentId, String search) {
        if (search == null || search.trim().isEmpty()) {
            return categoryRepository.findByParentIdOrderByCategoryNameAsc(parentId);
        }
        return categoryRepository.searchByName(search.trim()).stream()
                .filter(c -> parentId != null && parentId.equals(c.getParentId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("mainCategories", categoryRepository.countByParentIdIsNull());
        counts.put("subcategories", categoryRepository.count() - categoryRepository.countByParentIdIsNull());
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCategory(Integer id) {
        return requireFound(categoryRepository.findById(id), "Category not found with id: " + id);
    }

    @Override
    @Transactional
    public Category createMainCategory(
            String categoryName,
            String hsnCode,
            BigDecimal gstPercentage,
            String categoryImage,
            String mobileImage,
            String bannerImage,
            Boolean status) {
        String name = requireNonBlank(categoryName, "Category name");
        Category category = new Category();
        category.setCategoryName(name.trim());
        category.setParentId(null);
        category.setHsnCode(blankToNull(hsnCode));
        category.setGstPercentage(gstPercentage);
        category.setCategoryImage(catalogImageStorageService.normalizeCategoryImageValue(categoryImage));
        category.setMobileImage(catalogImageStorageService.normalizeCategoryImageValue(mobileImage));
        category.setBannerImage(catalogImageStorageService.normalizeCategoryImageValue(bannerImage));
        category.setStatus(status != null ? status : true);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category createSubcategory(
            Integer parentId,
            String categoryName,
            String hsnCode,
            BigDecimal gstPercentage,
            String categoryImage,
            String mobileImage,
            String bannerImage,
            Boolean status) {
        if (parentId == null) {
            throw new IllegalArgumentException("Main category is required.");
        }
        requireFound(categoryRepository.findById(parentId), "Main category not found with id: " + parentId);
        String name = requireNonBlank(categoryName, "Category name");
        Category category = new Category();
        category.setParentId(parentId);
        category.setCategoryName(name.trim());
        category.setHsnCode(blankToNull(hsnCode));
        category.setGstPercentage(gstPercentage);
        category.setCategoryImage(catalogImageStorageService.normalizeCategoryImageValue(categoryImage));
        category.setMobileImage(catalogImageStorageService.normalizeCategoryImageValue(mobileImage));
        category.setBannerImage(catalogImageStorageService.normalizeCategoryImageValue(bannerImage));
        category.setStatus(status != null ? status : true);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Integer id) {
        categoryRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void updateCategory(
            Integer id,
            String categoryName,
            String hsnCode,
            BigDecimal gstPercentage,
            String categoryImage,
            String mobileImage,
            String bannerImage,
            Boolean status) {
        Category category = getCategory(id);
        if (categoryName != null && !categoryName.isBlank()) {
            category.setCategoryName(categoryName.trim());
        }
        category.setHsnCode(blankToNull(hsnCode));
        category.setGstPercentage(gstPercentage);
        String normalizedImage = catalogImageStorageService.normalizeCategoryImageValue(categoryImage);
        if (normalizedImage != null) {
            category.setCategoryImage(normalizedImage);
        }
        String normalizedMobile = catalogImageStorageService.normalizeCategoryImageValue(mobileImage);
        if (normalizedMobile != null) {
            category.setMobileImage(normalizedMobile);
        }
        String normalizedBanner = catalogImageStorageService.normalizeCategoryImageValue(bannerImage);
        if (normalizedBanner != null) {
            category.setBannerImage(normalizedBanner);
        }
        if (status != null) {
            category.setStatus(status);
        }
        categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category uploadImages(Integer id, MultipartFile image, MultipartFile mobileImage, MultipartFile bannerImage) {
        Category category = getCategory(id);
        boolean changed = false;
        if (image != null && !image.isEmpty()) {
            category.setCategoryImage(catalogImageStorageService.storeCategoryImage(image));
            changed = true;
        }
        if (mobileImage != null && !mobileImage.isEmpty()) {
            category.setMobileImage(catalogImageStorageService.storeCategoryImage(mobileImage));
            changed = true;
        }
        if (bannerImage != null && !bannerImage.isEmpty()) {
            category.setBannerImage(catalogImageStorageService.storeCategoryImage(bannerImage));
            changed = true;
        }
        if (!changed) {
            throw new IllegalArgumentException("At least one image file is required");
        }
        return categoryRepository.save(category);
    }
}
