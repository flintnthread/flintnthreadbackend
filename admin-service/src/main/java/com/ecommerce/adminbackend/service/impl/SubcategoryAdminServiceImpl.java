package com.ecommerce.adminbackend.service.impl;

import com.ecommerce.adminbackend.entity.Subcategory;
import com.ecommerce.adminbackend.repository.SubcategoryRepository;
import com.ecommerce.adminbackend.service.CatalogImageStorageService;
import com.ecommerce.adminbackend.service.SubcategoryAdminService;
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
public class SubcategoryAdminServiceImpl implements SubcategoryAdminService {

    private final SubcategoryRepository subcategoryRepository;
    private final CatalogImageStorageService catalogImageStorageService;

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
    public Subcategory getSubcategory(Integer id) {
        return subcategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subcategory not found with id: " + id));
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
    public Subcategory createSubcategory(
            Integer categoryId,
            String subcategoryName,
            String subcategoryImage,
            String mobileImage,
            String materialSlabs,
            String weightSlabs,
            BigDecimal gstPercentage,
            Boolean status) {
        Subcategory subcategory = new Subcategory();
        subcategory.setCategoryId(categoryId);
        subcategory.setSubcategoryName(subcategoryName);
        subcategory.setSubcategoryImage(catalogImageStorageService.normalizeSubcategoryImageValue(subcategoryImage));
        subcategory.setMobileImage(catalogImageStorageService.normalizeSubcategoryImageValue(mobileImage));
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
    public Subcategory updateSubcategory(
            Integer id,
            Integer categoryId,
            String subcategoryName,
            String subcategoryImage,
            String mobileImage,
            String materialSlabs,
            String weightSlabs,
            BigDecimal gstPercentage,
            Boolean status) {
        Subcategory subcategory = getSubcategory(id);
        subcategory.setCategoryId(categoryId);
        subcategory.setSubcategoryName(subcategoryName);

        // Only overwrite image fields when a new value is provided (keeps existing on edit).
        String normalizedImage = catalogImageStorageService.normalizeSubcategoryImageValue(subcategoryImage);
        if (normalizedImage != null) {
            subcategory.setSubcategoryImage(normalizedImage);
        }
        String normalizedMobile = catalogImageStorageService.normalizeSubcategoryImageValue(mobileImage);
        if (normalizedMobile != null) {
            subcategory.setMobileImage(normalizedMobile);
        }

        subcategory.setMaterialSlabs(materialSlabs);
        subcategory.setWeightSlabs(weightSlabs);
        subcategory.setGstPercentage(gstPercentage);
        if (status != null) {
            subcategory.setStatus(status);
        }
        return subcategoryRepository.save(subcategory);
    }

    @Override
    @Transactional
    public Subcategory uploadImages(Integer id, MultipartFile image, MultipartFile mobileImage) {
        Subcategory subcategory = getSubcategory(id);
        boolean changed = false;
        if (image != null && !image.isEmpty()) {
            subcategory.setSubcategoryImage(catalogImageStorageService.storeSubcategoryImage(image));
            changed = true;
        }
        if (mobileImage != null && !mobileImage.isEmpty()) {
            subcategory.setMobileImage(catalogImageStorageService.storeSubcategoryImage(mobileImage));
            changed = true;
        }
        if (!changed) {
            throw new IllegalArgumentException("At least one image file is required");
        }
        return subcategoryRepository.save(subcategory);
    }
}
