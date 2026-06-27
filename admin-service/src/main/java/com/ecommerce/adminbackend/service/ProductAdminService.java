package com.ecommerce.adminbackend.service;

import com.ecommerce.adminbackend.common.PageResponse;

import java.util.Map;

public interface ProductAdminService {

    PageResponse<Map<String, Object>> listPending(int page, int size);

    PageResponse<Map<String, Object>> listProducts(
            String status,
            String search,
            Long sellerId,
            Boolean adminOnly,
            Integer mainCategoryId,
            Integer categoryId,
            Integer subcategoryId,
            int page,
            int size);

    Map<String, Object> stats();

    Map<String, Object> catalog();

    Map<String, Object> getProduct(Long id);

    Map<String, Object> approve(Long id, String note);

    Map<String, Object> reject(Long id, String note);
}
