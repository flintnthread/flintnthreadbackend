package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.entity.Subcategory;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SubcategoryAdminService {

    List<Subcategory> listSubcategories(Integer categoryId, String search);

    Subcategory getSubcategory(Integer id);

    Map<String, Long> getCounts();

    Subcategory createSubcategory(
            Integer categoryId,
            String subcategoryName,
            String subcategoryImage,
            String mobileImage,
            String materialSlabs,
            String weightSlabs,
            BigDecimal gstPercentage,
            Boolean status);

    void deleteSubcategory(Integer id);

    Subcategory updateSubcategory(
            Integer id,
            Integer categoryId,
            String subcategoryName,
            String subcategoryImage,
            String mobileImage,
            String materialSlabs,
            String weightSlabs,
            BigDecimal gstPercentage,
            Boolean status);

    Subcategory uploadImages(Integer id, MultipartFile image, MultipartFile mobileImage);
}
