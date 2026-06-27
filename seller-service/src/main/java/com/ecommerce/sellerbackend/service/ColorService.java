package com.ecommerce.sellerbackend.service;

import com.ecommerce.sellerbackend.dto.ColorRequest;
import com.ecommerce.sellerbackend.dto.ColorResponse;
import java.util.List;

public interface ColorService {

    List<ColorResponse> listForSeller(Long sellerId);

    ColorResponse create(Long sellerId, ColorRequest request);

    ColorResponse update(Long sellerId, Long id, ColorRequest request);

    void delete(Long sellerId, Long id);
}
