package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.entity.Subcategory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SubcategoryAdminService {

    List<Subcategory> listSubcategories(Integer categoryId, String search);

    Map<String, Long> getCounts();

    Subcategory createSubcategory(Integer categoryId, String subcategoryName, String subcategoryImage, String mobileImage, String materialSlabs, String weightSlabs, BigDecimal gstPercentage, Boolean status);

    void deleteSubcategory(Integer id);

    void updateSubcategory(Integer id, Integer categoryId, String subcategoryName, String subcategoryImage, String mobileImage, String materialSlabs, String weightSlabs, BigDecimal gstPercentage, Boolean status);
}
