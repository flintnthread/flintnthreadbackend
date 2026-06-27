package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.Subcategory;
import com.ecommerce.adminbackend.repository.SubcategoryRepository;
import com.ecommerce.adminbackend.service.SubcategoryAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubcategoryAdminServiceImpl implements SubcategoryAdminService {

    private final SubcategoryRepository subcategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Subcategory> listSubcategories(Integer categoryId, String search) {
        if (search != null && !search.isBlank()) {
            return subcategoryRepository.findBySubcategoryNameContainingIgnoreCase(search.trim());
        }
        if (categoryId != null) {
            return subcategoryRepository.findByCategoryIdOrderBySubcategoryNameAsc(categoryId);
        }
        return subcategoryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("total", subcategoryRepository.count());
        counts.put("active", subcategoryRepository.findByStatus(true).stream().count());
        counts.put("inactive", subcategoryRepository.findByStatus(false).stream().count());
        return counts;
    }

    @Override
    @Transactional
    public Subcategory createSubcategory(Integer categoryId, String subcategoryName, String subcategoryImage, String mobileImage, String materialSlabs, String weightSlabs, BigDecimal gstPercentage, Boolean status) {
        Subcategory subcategory = new Subcategory();
        subcategory.setCategoryId(categoryId);
        subcategory.setSubcategoryName(subcategoryName);
        subcategory.setSubcategoryImage(subcategoryImage);
        subcategory.setMobileImage(mobileImage);
        subcategory.setMaterialSlabs(materialSlabs);
        subcategory.setWeightSlabs(weightSlabs);
        subcategory.setGstPercentage(gstPercentage);
        subcategory.setStatus(status != null ? status : true);
        return subcategoryRepository.save(subcategory);
    }

    @Override
    @Transactional
    public void deleteSubcategory(Integer id) {
        subcategoryRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void updateSubcategory(Integer id, Integer categoryId, String subcategoryName, String subcategoryImage, String mobileImage, String materialSlabs, String weightSlabs, BigDecimal gstPercentage, Boolean status) {
        Subcategory subcategory = subcategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subcategory not found with id: " + id));
        subcategory.setCategoryId(categoryId);
        subcategory.setSubcategoryName(subcategoryName);
        subcategory.setSubcategoryImage(subcategoryImage);
        subcategory.setMobileImage(mobileImage);
        subcategory.setMaterialSlabs(materialSlabs);
        subcategory.setWeightSlabs(weightSlabs);
        subcategory.setGstPercentage(gstPercentage);
        subcategory.setStatus(status);
        subcategoryRepository.save(subcategory);
    }
}
