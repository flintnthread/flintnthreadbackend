package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.SubCategoryResponseDTO;

import java.util.List;

    public interface SubCategoryService {

        List<SubCategoryResponseDTO> getSubCategories(Long categoryId);

    }

