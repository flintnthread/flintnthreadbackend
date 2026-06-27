package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.FaqCategoryRequest;
import com.ecommerce.authdemo.dto.FaqCategoryResponse;

import java.util.List;

public interface FaqCategoryService {
    FaqCategoryResponse create(FaqCategoryRequest request);

    List<FaqCategoryResponse> getAll(Boolean status);

    FaqCategoryResponse update(Integer id, FaqCategoryRequest request);

    FaqCategoryResponse updateStatus(Integer id, Boolean status);

    void delete(Integer id);
}
