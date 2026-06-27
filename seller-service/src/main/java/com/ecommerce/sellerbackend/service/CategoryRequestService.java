package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.CategoryRequestPayload;
import com.ecommerce.sellerbackend.dto.CategoryRequestResponse;
import java.util.List;

public interface CategoryRequestService {

    List<CategoryRequestResponse> listForSeller(Long sellerId);

    CategoryRequestResponse create(Long sellerId, CategoryRequestPayload payload);

    CategoryRequestResponse update(Long sellerId, Long requestId, CategoryRequestPayload payload);

    void delete(Long sellerId, Long requestId);
}
