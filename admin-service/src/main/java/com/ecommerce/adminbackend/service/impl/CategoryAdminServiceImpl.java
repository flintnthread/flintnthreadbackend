package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.Category;
import com.ecommerce.adminbackend.repository.CategoryRepository;
import com.ecommerce.adminbackend.service.CategoryAdminService;
import com.ecommerce.adminbackend.service.support.BaseAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryAdminServiceImpl extends BaseAdminService implements CategoryAdminService {

    private final CategoryRepository categoryRepository;

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
                .filter(c -> parentId.equals(c.getParentId()))
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
    @Transactional
    public Category createMainCategory(String categoryName, String hsnCode, BigDecimal gstPercentage, String categoryImage, String mobileImage, String bannerImage, Boolean status) {
        Category category = new Category();
        category.setCategoryName(categoryName);
        category.setParentId(null);
        category.setHsnCode(hsnCode);
        category.setGstPercentage(gstPercentage);
        category.setCategoryImage(categoryImage);
        category.setMobileImage(mobileImage);
        category.setBannerImage(bannerImage);
        category.setStatus(status != null ? status : true);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category createSubcategory(Integer parentId, String categoryName, String hsnCode, BigDecimal gstPercentage, String categoryImage, String mobileImage, String bannerImage, Boolean status) {
        Category category = new Category();
        category.setParentId(parentId);
        category.setCategoryName(categoryName);
        category.setHsnCode(hsnCode);
        category.setGstPercentage(gstPercentage);
        category.setCategoryImage(categoryImage);
        category.setMobileImage(mobileImage);
        category.setBannerImage(bannerImage);
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
    public void updateCategory(Integer id, String categoryName, String hsnCode, BigDecimal gstPercentage, String categoryImage, String mobileImage, String bannerImage, Boolean status) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));
        category.setCategoryName(categoryName);
        category.setHsnCode(hsnCode);
        category.setGstPercentage(gstPercentage);
        category.setCategoryImage(categoryImage);
        category.setMobileImage(mobileImage);
        category.setBannerImage(bannerImage);
        category.setStatus(status);
        categoryRepository.save(category);
    }
}
