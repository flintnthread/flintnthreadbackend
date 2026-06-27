package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.SizeRequest;
import com.ecommerce.sellerbackend.dto.SizeResponse;
import java.util.List;

public interface SizeService {

    List<SizeResponse> listForSeller(Long sellerId);

    SizeResponse create(Long sellerId, SizeRequest request);

    SizeResponse update(Long sellerId, Long id, SizeRequest request);

    void delete(Long sellerId, Long id);
}
