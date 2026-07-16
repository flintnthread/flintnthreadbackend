package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.entity.Category;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CategoryAdminService {

    List<Category> listMainCategories(String search);

    List<Category> listSubcategories(Integer parentId, String search);

    Map<String, Long> getCounts();

    Category createMainCategory(String categoryName, String hsnCode, BigDecimal gstPercentage, String categoryImage, String mobileImage, String bannerImage, Boolean status);

    Category createSubcategory(Integer parentId, String categoryName, String hsnCode, BigDecimal gstPercentage, String categoryImage, String mobileImage, String bannerImage, Boolean status);

    void deleteCategory(Integer id);

    void updateCategory(Integer id, String categoryName, String hsnCode, BigDecimal gstPercentage, String categoryImage, String mobileImage, String bannerImage, Boolean status);

    Category getCategory(Integer id);

    Category uploadImages(Integer id, MultipartFile image, MultipartFile mobileImage, MultipartFile bannerImage);
}
