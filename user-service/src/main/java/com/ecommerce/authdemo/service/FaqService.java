package com.ecommerce.authdemo.service;

import com.ecommerce.authdemo.dto.FaqRequest;
import com.ecommerce.authdemo.dto.FaqResponse;

import java.util.List;

public interface FaqService {
    FaqResponse create(FaqRequest request);

    List<FaqResponse> getAll(Integer categoryId, Boolean status);

    FaqResponse update(Integer id, FaqRequest request);

    FaqResponse updateStatus(Integer id, Boolean status);

    void delete(Integer id);
}
